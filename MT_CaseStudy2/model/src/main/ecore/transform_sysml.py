#!/usr/bin/env python3
"""
Transform a MagicDraw SysML XMI export into a valid XMI instance of the sysml.ecore metamodel.

Usage:
    python3 transform_sysml.py <input.xml> <output.xmi>

The input is a MagicDraw/Cameo XMI export (UML 2.x + SysML stereotypes).
The output is an EMF XMI instance conforming to:
    nsURI = http://vitruv.tools/methodologisttemplate/sysml
"""

import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element, SubElement
import sys

# ── Source namespaces ───────────────────────────────────────────────────────
SRC_XMI  = 'http://www.omg.org/spec/XMI/20131001'
SRC_UML  = 'http://www.omg.org/spec/UML/20131001'
SRC_SYSML = 'http://www.omg.org/spec/SysML/20181001/SysML'

# ── Target (output) namespaces ──────────────────────────────────────────────
OUT_SYSML = 'http://vitruv.tools/methodologisttemplate/sysml'
OUT_XMI   = 'http://www.omg.org/XMI'
XSI       = 'http://www.w3.org/2001/XMLSchema-instance'

# ── Helpers ─────────────────────────────────────────────────────────────────

def qn(ns, local):
    """Qualified name helper."""
    return f'{{{ns}}}{local}'

def src_xmi_id(elem):
    return elem.get(qn(SRC_XMI, 'id'), '')

def src_xmi_type(elem):
    """Return the xmi:type attribute value (e.g. 'uml:Class')."""
    return elem.get(qn(SRC_XMI, 'type'), '')

def out_set_id(elem, id_val):
    """Set both the EMF xmi:id (object identity) and the model 'id' attribute."""
    if id_val:
        elem.set(qn(OUT_XMI, 'id'), id_val)
        elem.set('id', id_val)

def ref(id_val):
    """Return a same-document fragment URI for a given id."""
    return f'#{id_val}' if id_val else ''


# ── Main transformer ─────────────────────────────────────────────────────────

class Transformer:
    def __init__(self, src_root):
        self.src_root = src_root

        # Stereotype-application lookup maps
        self.block_ids        : dict = {}   # UML Class id → {}
        self.req_ids          : dict = {}   # UML Class id → {text, reqId}
        self.flowprop_ids     : dict = {}   # UML Property id → {direction}
        self.valuetype_ids    : set  = set()  # UML DataType ids
        self.constraintblk_ids: set  = set()  # UML Class ids (ConstraintBlock)
        self.iface_block_ids  : set  = set()  # UML Interface ids (InterfaceBlock)

        self._collect_stereotypes()

    # ── Stereotype collection ────────────────────────────────────────────────

    def _collect_stereotypes(self):
        """Scan all top-level XMI children for SysML stereotype applications."""
        for child in self.src_root:
            tag = child.tag
            if tag == qn(SRC_SYSML, 'Block'):
                base = child.get('base_Class', '')
                if base:
                    self.block_ids[base] = {}
            elif tag == qn(SRC_SYSML, 'Requirement'):
                base = child.get('base_Class', '')
                if base:
                    self.req_ids[base] = {
                        'text'  : child.get('Text', ''),
                        'reqId' : child.get('Id', ''),
                    }
            elif tag == qn(SRC_SYSML, 'FlowProperty'):
                base = child.get('base_Property', '')
                if base:
                    self.flowprop_ids[base] = {
                        'direction': child.get('direction', 'in'),
                    }
            elif tag == qn(SRC_SYSML, 'ValueType'):
                base = child.get('base_DataType', '')
                if base:
                    self.valuetype_ids.add(base)
            elif tag == qn(SRC_SYSML, 'ConstraintBlock'):
                base = child.get('base_Class', '')
                if base:
                    self.constraintblk_ids.add(base)
            elif tag == qn(SRC_SYSML, 'InterfaceBlock'):
                base = child.get('base_Class', '')
                if base:
                    self.iface_block_ids.add(base)

    # ── Root transform entry point ───────────────────────────────────────────

    def transform(self):
        """Run the transformation and return the output root Element."""
        # Register namespace prefixes for pretty serialisation
        ET.register_namespace('sysml', OUT_SYSML)
        ET.register_namespace('xmi',   OUT_XMI)
        ET.register_namespace('xsi',   XSI)

        # Find the single uml:Model element inside the XMI root
        uml_model = None
        for child in self.src_root:
            if child.tag == qn(SRC_UML, 'Model'):
                uml_model = child
                break
        if uml_model is None:
            raise ValueError('No uml:Model element found in source XMI')

        # Build output root  <sysml:Model …>
        # Note: ET.register_namespace() above already takes care of namespace
        # declarations; we must NOT also set them as explicit attributes, or
        # the serialiser emits them twice.
        out_root = Element(qn(OUT_SYSML, 'Model'))
        out_root.set(qn(OUT_XMI, 'version'), '2.0')
        out_root.set('name', uml_model.get('name', 'Model'))
        out_set_id(out_root, src_xmi_id(uml_model))

        # Recursively populate
        self._process_pkg_contents(uml_model, out_root)

        return out_root

    # ── Package traversal ────────────────────────────────────────────────────

    def _process_pkg_contents(self, src_pkg, out_pkg):
        """Iterate over the direct children of a UML package/model and map them."""
        for child in src_pkg:
            tag  = child.tag
            typ  = src_xmi_type(child)

            # Skip extension / comment / import boilerplate
            if tag in (
                qn(SRC_XMI, 'Extension'),
                qn(SRC_XMI, 'Documentation'),
                'ownedComment',
                'packageImport',
                'elementImport',
            ):
                continue

            # Direct uml: children (rare, but keep it consistent)
            if tag == qn(SRC_UML, 'Package'):
                self._add_package(child, out_pkg)
                continue

            # The common case: packagedElement / ownedMember / nestedClassifier
            if tag in ('packagedElement', 'ownedMember', 'nestedClassifier',
                       'ownedType', 'ownedBehavior'):
                self._map_element(child, out_pkg)

    def _map_element(self, src_elem, out_pkg):
        """Route a single source element to the right mapping method."""
        typ  = src_xmi_type(src_elem)
        eid  = src_xmi_id(src_elem)

        if typ in ('uml:Package', 'uml:Profile', 'uml:Model'):
            self._add_package(src_elem, out_pkg)

        elif typ == 'uml:Class':
            if eid in self.constraintblk_ids:
                self._add_constraint_block(src_elem, out_pkg)
            elif eid in self.req_ids:
                self._add_requirement(src_elem, out_pkg)
            else:
                # All other UML Classes (including un-stereotyped ones) → Block
                self._add_block(src_elem, out_pkg)

        elif typ == 'uml:Interface':
            # InterfaceBlock if explicitly stereotyped, otherwise generic Block
            self._add_block(src_elem, out_pkg,
                             sysml_type='sysml:InterfaceBlock'
                             if eid in self.iface_block_ids else 'sysml:Block')

        elif typ == 'uml:DataType':
            if eid in self.valuetype_ids:
                self._add_value_type(src_elem, out_pkg)
            # else: skip un-stereotyped data types (e.g. primitive type imports)

        elif typ == 'uml:Association':
            self._add_association(src_elem, out_pkg)

        elif typ == 'uml:Activity':
            self._add_activity(src_elem, out_pkg)

        elif typ in ('uml:StateMachine',):
            self._add_state_machine(src_elem, out_pkg)

        # uml:Enumeration, uml:PrimitiveType, uml:Signal, …
        # not directly represented in the ecore – skip

    # ── Package ──────────────────────────────────────────────────────────────

    def _add_package(self, src_pkg, out_parent):
        out_pkg = SubElement(out_parent, 'nestedPackages')
        out_pkg.set(qn(XSI, 'type'), 'sysml:Package')
        out_pkg.set('name', src_pkg.get('name', ''))
        out_set_id(out_pkg, src_xmi_id(src_pkg))
        self._process_pkg_contents(src_pkg, out_pkg)

    # ── Block ─────────────────────────────────────────────────────────────────

    def _add_block(self, src_class, out_pkg, sysml_type='sysml:Block'):
        out_blk = SubElement(out_pkg, 'ownedElements')
        out_blk.set(qn(XSI, 'type'), sysml_type)
        out_blk.set('name', src_class.get('name', ''))
        out_set_id(out_blk, src_xmi_id(src_class))

        enc = src_class.get('isEncapsulated', '')
        if enc:
            out_blk.set('isEncapsulated', enc)

        self._fill_block_contents(src_class, out_blk)

    def _fill_block_contents(self, src_class, out_blk):
        """Process the structural children of a UML Class into a Block."""
        for child in src_class:
            tag = child.tag
            typ = src_xmi_type(child)

            if tag == qn(SRC_XMI, 'Extension'):
                continue

            if tag == 'ownedAttribute':
                if typ == 'uml:Port':
                    self._add_port(child, out_blk)
                else:
                    self._add_property(child, out_blk)

            elif tag == 'ownedPort':
                self._add_port(child, out_blk)

            elif tag == 'ownedConnector':
                self._add_connector(child, out_blk)

            elif tag == 'ownedOperation':
                self._add_operation(child, out_blk)

            elif tag == 'ownedReception':
                self._add_reception(child, out_blk)

            elif tag == 'ownedBehavior':
                # behaviors owned by a block – not directly containable in the ecore,
                # but we can nest them as activities/state-machines in the enclosing package.
                # Here we silently skip them to keep the model well-formed.
                pass

            elif tag == 'generalization':
                # We emit Generalization elements at the package level below
                pass

    # ── InterfaceBlock ────────────────────────────────────────────────────────

    def _add_interface_block(self, src_iface, out_pkg):
        out_ib = SubElement(out_pkg, 'ownedElements')
        out_ib.set(qn(XSI, 'type'), 'sysml:InterfaceBlock')
        out_ib.set('name', src_iface.get('name', ''))
        out_set_id(out_ib, src_xmi_id(src_iface))
        self._fill_block_contents(src_iface, out_ib)

    # ── ConstraintBlock ───────────────────────────────────────────────────────

    def _add_constraint_block(self, src_class, out_pkg):
        out_cb = SubElement(out_pkg, 'ownedElements')
        out_cb.set(qn(XSI, 'type'), 'sysml:ConstraintBlock')
        out_cb.set('name', src_class.get('name', ''))
        out_set_id(out_cb, src_xmi_id(src_class))

        for child in src_class:
            if child.tag == 'ownedAttribute' and src_xmi_type(child) != 'uml:Port':
                cid   = src_xmi_id(child)
                cname = child.get('name', '')
                ctype = child.get('type', '')

                out_cp = SubElement(out_cb, 'constraintParameters')
                out_cp.set('name', cname)
                out_set_id(out_cp, cid)
                if ctype:
                    out_cp.set('type', ref(ctype))

    # ── ValueType ─────────────────────────────────────────────────────────────

    def _add_value_type(self, src_dt, out_pkg):
        out_vt = SubElement(out_pkg, 'ownedElements')
        out_vt.set(qn(XSI, 'type'), 'sysml:ValueType')
        out_vt.set('name', src_dt.get('name', ''))
        out_set_id(out_vt, src_xmi_id(src_dt))

    # ── Association ───────────────────────────────────────────────────────────

    def _add_association(self, src_assoc, out_pkg):
        out_a = SubElement(out_pkg, 'ownedElements')
        out_a.set(qn(XSI, 'type'), 'sysml:Association')
        name = src_assoc.get('name', '')
        if name:
            out_a.set('name', name)
        out_set_id(out_a, src_xmi_id(src_assoc))
        is_derived = src_assoc.get('isDerived', '')
        if is_derived:
            out_a.set('isDerived', is_derived)

        # Gather the two ends to resolve source/target for the Relationship fields.
        # In UML an Association owns its member ends; we store the class that
        # navigates to the other side as 'source', and the target type as 'target'.
        ends = [c for c in src_assoc if c.tag == 'ownedEnd']
        if len(ends) >= 2:
            s_type = ends[0].get('type', '')
            t_type = ends[1].get('type', '')
            if s_type:
                out_a.set('source', ref(s_type))
            if t_type:
                out_a.set('target', ref(t_type))

    # ── Properties (Part / Reference / Value / FlowProperty) ──────────────────

    def _add_property(self, src_prop, out_blk):
        pid   = src_xmi_id(src_prop)
        pname = src_prop.get('name', '')
        agg   = src_prop.get('aggregation', 'none')
        ptype = src_prop.get('type', '')
        mult  = self._multiplicity(src_prop)
        default = self._default_value(src_prop)

        common_attrs = {}
        if pname:
            common_attrs['name'] = pname
        if ptype:
            common_attrs['type'] = ref(ptype)
        if mult:
            common_attrs['multiplicity'] = mult

        if pid in self.flowprop_ids:
            fp = self.flowprop_ids[pid]
            out_fp = SubElement(out_blk, 'flowProperties')
            out_fp.set('name', pname)
            out_set_id(out_fp, pid)
            out_fp.set('direction', fp['direction'])
            if ptype:
                out_fp.set('type', ref(ptype))
            if mult:
                out_fp.set('multiplicity', mult)

        elif agg == 'composite':
            out_pp = SubElement(out_blk, 'parts')
            out_pp.set('name', pname)
            out_set_id(out_pp, pid)
            if ptype:
                out_pp.set('type', ref(ptype))
            if mult:
                out_pp.set('multiplicity', mult)

        elif ptype:
            # Reference to another element
            out_rp = SubElement(out_blk, 'references')
            out_rp.set('name', pname)
            out_set_id(out_rp, pid)
            out_rp.set('type', ref(ptype))
            if mult:
                out_rp.set('multiplicity', mult)

        else:
            # Untyped property → ValueProperty
            out_vp = SubElement(out_blk, 'values')
            out_vp.set('name', pname)
            out_set_id(out_vp, pid)
            if default:
                out_vp.set('defaultValue', default)
            if mult:
                out_vp.set('multiplicity', mult)

    def _multiplicity(self, src_elem):
        lower = src_elem.get('lower', '')
        upper = src_elem.get('upper', '')
        if not lower and not upper:
            return ''
        lo = lower or '1'
        hi = upper or '1'
        return lo if lo == hi else f'{lo}..{hi}'

    def _default_value(self, src_elem):
        for child in src_elem:
            if child.tag == 'defaultValue':
                v = child.get('value', '') or child.get('body', '')
                return v
        return ''

    # ── Port ──────────────────────────────────────────────────────────────────

    def _add_port(self, src_port, out_blk):
        pid   = src_xmi_id(src_port)
        pname = src_port.get('name', '')
        ptype = src_port.get('type', '')

        # Default to ProxyPort; could be refined with more stereotype data
        out_port = SubElement(out_blk, 'ports')
        out_port.set(qn(XSI, 'type'), 'sysml:ProxyPort')
        out_port.set('name', pname)
        out_set_id(out_port, pid)

        if ptype:
            out_port.set('type', ref(ptype))

        conj = src_port.get('isConjugated', '')
        if conj:
            out_port.set('isConjugated', conj)

        beh = src_port.get('isBehavior', '')
        if beh:
            out_port.set('isBehavior', beh)

        svc = src_port.get('isService', '')
        if svc:
            out_port.set('isService', svc)

    # ── Connector ─────────────────────────────────────────────────────────────

    def _add_connector(self, src_conn, out_blk):
        cid   = src_xmi_id(src_conn)
        cname = src_conn.get('name', '')

        out_conn = SubElement(out_blk, 'connectors')
        if cname:
            out_conn.set('name', cname)
        out_set_id(out_conn, cid)

        for child in src_conn:
            if child.tag == 'end':
                eid  = src_xmi_id(child)
                role = child.get('role', '')
                pwp  = child.get('partWithPort', '')

                out_end = SubElement(out_conn, 'ends')
                out_set_id(out_end, eid)
                if role:
                    out_end.set('role', ref(role))
                if pwp:
                    out_end.set('partWithPort', ref(pwp))

    # ── Operation ─────────────────────────────────────────────────────────────

    def _add_operation(self, src_op, out_blk):
        oid   = src_xmi_id(src_op)
        oname = src_op.get('name', '')

        out_op = SubElement(out_blk, 'operations')
        out_op.set('name', oname)
        out_set_id(out_op, oid)

        is_query = src_op.get('isQuery', '')
        if is_query:
            out_op.set('isQuery', is_query)

        for child in src_op:
            if child.tag == 'ownedParameter':
                param_id  = src_xmi_id(child)
                param_dir = child.get('direction', 'in')
                param_type = child.get('type', '')

                out_p = SubElement(out_op, 'parameters')
                pname = child.get('name', '')
                if pname:
                    out_p.set('name', pname)
                out_set_id(out_p, param_id)
                out_p.set('direction', param_dir)
                if param_type:
                    out_p.set('type', ref(param_type))

    # ── Reception ─────────────────────────────────────────────────────────────

    def _add_reception(self, src_rec, out_blk):
        rid    = src_xmi_id(src_rec)
        signal = src_rec.get('signal', '')
        rname  = src_rec.get('name', signal)

        out_rec = SubElement(out_blk, 'receptions')
        if rname:
            out_rec.set('name', rname)
        out_set_id(out_rec, rid)
        if signal:
            out_rec.set('signal', ref(signal))

    # ── Requirement ───────────────────────────────────────────────────────────

    def _add_requirement(self, src_class, out_pkg):
        eid   = src_xmi_id(src_class)
        ename = src_class.get('name', '')
        rinfo = self.req_ids.get(eid, {})

        out_req = SubElement(out_pkg, 'ownedElements')
        out_req.set(qn(XSI, 'type'), 'sysml:Requirement')
        out_req.set('name', ename)
        out_set_id(out_req, eid)

        # 'text' is lowerBound=1 (required) in the ecore
        text = rinfo.get('text', '') or ename or 'No text provided'
        out_req.set('text', text)

        req_id_str = rinfo.get('reqId', '')
        if req_id_str:
            out_req.set('id', req_id_str)   # user-visible Requirement ID

        # Process nested requirements
        for child in src_class:
            tag = child.tag
            typ = src_xmi_type(child)
            cid = src_xmi_id(child)
            if tag in ('nestedClassifier', 'packagedElement', 'ownedMember') and typ == 'uml:Class':
                if cid in self.req_ids:
                    self._add_requirement(child, out_pkg)

    # ── Activity (basic) ──────────────────────────────────────────────────────

    def _add_activity(self, src_act, out_pkg):
        aid   = src_xmi_id(src_act)
        aname = src_act.get('name', '')

        out_act = SubElement(out_pkg, 'ownedElements')
        out_act.set(qn(XSI, 'type'), 'sysml:Activity')
        out_act.set('name', aname)
        out_set_id(out_act, aid)

        # We emit only the activity shell; detailed node/edge mapping is omitted
        # to keep the output well-formed (ActivityNode and ActivityEdge are
        # abstract and require concrete subtypes not present in this ecore).

    # ── StateMachine (basic) ──────────────────────────────────────────────────

    def _add_state_machine(self, src_sm, out_pkg):
        sid   = src_xmi_id(src_sm)
        sname = src_sm.get('name', '')

        out_sm = SubElement(out_pkg, 'ownedElements')
        out_sm.set(qn(XSI, 'type'), 'sysml:StateMachine')
        out_sm.set('name', sname)
        out_set_id(out_sm, sid)

        for region_elem in src_sm:
            if region_elem.tag == 'region':
                rid   = src_xmi_id(region_elem)
                rname = region_elem.get('name', '')

                out_reg = SubElement(out_sm, 'regions')
                out_reg.set('name', rname)
                out_set_id(out_reg, rid)

                for child in region_elem:
                    tag = child.tag
                    typ = src_xmi_type(child)

                    if tag == 'subvertex':
                        vid   = src_xmi_id(child)
                        vname = child.get('name', '')
                        if typ in ('uml:State', 'uml:FinalState', 'uml:Pseudostate', ''):
                            out_v = SubElement(out_reg, 'vertices')
                            out_v.set(qn(XSI, 'type'), 'sysml:State')
                            out_v.set('name', vname)
                            out_set_id(out_v, vid)

                    elif tag == 'transition':
                        tid   = src_xmi_id(child)
                        tname = child.get('name', '')
                        src_v = child.get('source', '')
                        tgt_v = child.get('target', '')

                        out_t = SubElement(out_reg, 'transitions')
                        if tname:
                            out_t.set('name', tname)
                        out_set_id(out_t, tid)
                        if src_v:
                            out_t.set('source', ref(src_v))
                        if tgt_v:
                            out_t.set('target', ref(tgt_v))

                        guard_elem = child.find('guard')
                        if guard_elem is not None:
                            spec = guard_elem.find('specification')
                            if spec is not None:
                                body = spec.get('body', '') or spec.get('value', '')
                                if body:
                                    out_t.set('guard', body)

                        effect_elem = child.find('effect')
                        if effect_elem is not None:
                            body = effect_elem.get('body', '') or effect_elem.get('name', '')
                            if body:
                                out_t.set('effect', body)


# ── Entry point ──────────────────────────────────────────────────────────────

def main():
    if len(sys.argv) < 3:
        print(f'Usage: {sys.argv[0]} <input.xml> <output.xmi>')
        sys.exit(1)

    input_file  = sys.argv[1]
    output_file = sys.argv[2]

    print(f'[1/3] Parsing {input_file} …')
    # Use large parser limit if needed (lxml not required – stdlib is sufficient)
    tree = ET.parse(input_file)
    root = tree.getroot()

    print('[2/3] Transforming …')
    transformer = Transformer(root)
    out_root = transformer.transform()

    print(f'[3/3] Writing {output_file} …')
    out_tree = ET.ElementTree(out_root)
    ET.indent(out_tree, space='  ')
    out_tree.write(output_file, encoding='unicode', xml_declaration=True)

    print('Done.')


if __name__ == '__main__':
    main()
