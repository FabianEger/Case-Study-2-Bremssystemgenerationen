"""Rebuild correspondence_diagram.html after the snapshots or the test output changed.

    python3 -I extract_snapshot_graph.py ../MT_CaseStudy2 initial_states.json   (after mvn clean verify, for the final state)
    python3 -I build_diagram.py
"""
import os
here = os.path.dirname(os.path.abspath(__file__))
template = open(os.path.join(here, 'template.html'), encoding='utf-8').read()
data = open(os.path.join(here, 'initial_states.json'), encoding='utf-8').read().replace('</', '<\\/')
open(os.path.join(here, 'correspondence_diagram.html'), 'w', encoding='utf-8').write(template.replace('/*DATA*/', data))
print('wrote correspondence_diagram.html')
