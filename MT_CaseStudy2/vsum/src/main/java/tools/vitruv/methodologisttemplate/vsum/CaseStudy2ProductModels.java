package tools.vitruv.methodologisttemplate.vsum;

import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.GN_PLUS_1;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.fileSuffix;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.modelUri;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.productArchitecture;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.productFunctions;
import static tools.vitruv.methodologisttemplate.vsum.CaseStudy2Vsum.productRequirements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import product_architecture.ComponentKind;
import product_architecture.ProductArchitectureRepository;
import product_architecture.ProductComponent;
import product_architecture.Product_architectureFactory;
import product_functions.ProductFunction;
import product_functions.ProductFunctionsRepository;
import product_functions.Product_functionsFactory;
import product_requirements.ProductRequirement;
import product_requirements.ProductRequirementsRepository;
import product_requirements.Product_requirementsFactory;
import product_requirements.RequirementKind;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;

/**
 * Case Study 2: instances of the product developer view ("Cameo-Systemmodell der Bremssystemgenerationen").
 *
 * <p>The content is taken from the Cameo exports {@code Bremssystem_Gn-1.xml}, {@code Bremssystem_Gn.xml} and
 * {@code Bremssystem_Gn+1_K1.xml}: requirement list (Id, name, text, functional / non-functional), the
 * {@code «Funktion»} tree with its generalisations and its allocations to requirements, and the
 * {@code «Struktur-Element»} blocks with their parts, allocated functions and satisfied requirements.
 * The {@link ComponentKind} of every component is not part of the Cameo model; it is assigned here
 * (engineering judgement) because CPRs "2" use it to phrase the derived validation need.
 *
 * <p>Elements that only appear in the scenario description of the slides (not in the Cameo export) are
 * marked with {@code [slide]}.
 *
 * <p>All methods operate on an open {@link CommittableView}; the caller commits.
 */
public final class CaseStudy2ProductModels {

  private CaseStudy2ProductModels() {
  }

  // ===========================================================================
  // Generation set-up
  // ===========================================================================

  /**
   * Registers the (empty) requirements, functions and architecture models of a product generation.
   *
   * @param predecessorGeneration generation the new one is derived from, or {@code null} for the baseline
   */
  public static void registerGeneration(CommittableView view, Path storageFolder, String generation,
      String predecessorGeneration) {
    ProductRequirementsRepository requirements = Product_requirementsFactory.eINSTANCE.createProductRequirementsRepository();
    ProductFunctionsRepository functions = Product_functionsFactory.eINSTANCE.createProductFunctionsRepository();
    ProductArchitectureRepository architecture = Product_architectureFactory.eINSTANCE.createProductArchitectureRepository();
    requirements.setGeneration(generation);
    functions.setGeneration(generation);
    architecture.setGeneration(generation);
    if (predecessorGeneration != null) {
      requirements.setPredecessor(productRequirements(view, predecessorGeneration));
      functions.setPredecessor(productFunctions(view, predecessorGeneration));
      architecture.setPredecessor(productArchitecture(view, predecessorGeneration));
    }
    String suffix = fileSuffix(generation);
    view.registerRoot(requirements, modelUri(storageFolder, "product_" + suffix + ".product_requirements"));
    view.registerRoot(functions, modelUri(storageFolder, "product_" + suffix + ".product_functions"));
    view.registerRoot(architecture, modelUri(storageFolder, "product_" + suffix + ".product_architecture"));
  }

  /**
   * Slide 3, step 2: the product developer takes over the system model of the predecessor generation as
   * the basis of the new generation (copy of all requirements, functions and components incl. their
   * internal relations).
   */
  public static void copyFromPredecessor(CommittableView view, String generation) {
    ProductRequirementsRepository requirements = productRequirements(view, generation);
    ProductFunctionsRepository functions = productFunctions(view, generation);
    ProductArchitectureRepository architecture = productArchitecture(view, generation);
    // Owners before owned elements: a commit must not reference an element that is inserted later.
    List<EObject> originals = new ArrayList<>();
    originals.addAll(ownersFirst(requirements.getPredecessor().getRequirements(), ProductRequirement::getParent));
    originals.addAll(ownersFirst(functions.getPredecessor().getFunctions(), ProductFunction::getGeneral));
    originals.addAll(ownersFirst(architecture.getPredecessor().getComponents(), ProductComponent::getParent));
    EcoreUtil.Copier copier = new EcoreUtil.Copier();
    List<EObject> copies = new ArrayList<>(copier.copyAll(originals));
    copier.copyReferences();
    for (EObject copy : copies) {
      if (copy instanceof ProductRequirement r) {
        requirements.getRequirements().add(r);
      } else if (copy instanceof ProductFunction f) {
        functions.getFunctions().add(f);
      } else if (copy instanceof ProductComponent c) {
        architecture.getComponents().add(c);
      }
    }
  }

  private static <T> List<T> ownersFirst(List<T> elements, java.util.function.Function<T, T> owner) {
    List<T> ordered = new ArrayList<>();
    List<T> remaining = new ArrayList<>(elements);
    while (!remaining.isEmpty()) {
      List<T> ready = remaining.stream()
          .filter(e -> owner.apply(e) == null || ordered.contains(owner.apply(e)) || !elements.contains(owner.apply(e)))
          .toList();
      if (ready.isEmpty()) {
        throw new IllegalStateException("Cyclic ownership in product model");
      }
      ordered.addAll(ready);
      remaining.removeAll(ready);
    }
    return ordered;
  }

  // ===========================================================================
  // Baseline: generation Gn-1 (brake system without ABS)
  // ===========================================================================

  /** Content of {@code Bremssystem_Gn-1.xml}. */
  public static void populateGnMinus1(CommittableView view, String generation) {
    ProductRequirementsRepository r = productRequirements(view, generation);
    ProductFunctionsRepository f = productFunctions(view, generation);
    ProductArchitectureRepository a = productArchitecture(view, generation);

    // --- Anforderungsliste
    req(r, "23", "Gesamtfahrzeug verzögern und feststellen", "Gesamtfahrzeug sowohl verzögern als auch feststellen können", RequirementKind.FUNCTIONAL, null);
    req(r, "23.1", "Eingabeeinrichtung", "Nur eine Eingabeeinrichtung darf vorhanden sein.\nMuss das Bremsen ermöglichen während beide Hände am Steuer sind.", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.1.1", "Eingabekraft verstärken", "Muss die Muskelkraft auf ein Maß erhöhen, sodass die geforderten Bremswerte erreicht werden", RequirementKind.FUNCTIONAL, "23.1");
    req(r, "23.1.2", "Bremsassistent", "Erkennt eine Gefahrenbremsung und unterstützt den Fahrer dabei eine Vollbremsung einzuleiten", RequirementKind.FUNCTIONAL, "23.1");
    req(r, "23.1.3", "Aus Metall gefertigt", "Muss aus Metall gefertigt sein, falls Versagen eine Hilfsbremsung unmöglich macht", RequirementKind.NON_FUNCTIONAL, "23.1");
    req(r, "23.2", "Räder verzögern", "Verzögern der Fahrzeugräder", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.2.1", "Automatische Nachstellung aufgrund Verschleiß", "Verschleißbedingte Toleranzen müssen automatisch ausgeglichen werden", RequirementKind.FUNCTIONAL, "23.2");
    req(r, "23.2.2", "Redundanz", "Aufteilung in Betriebsbrems- und Hilfsbremssystem.\nDabei muss HBS noch funktionsfähig sein wenn nicht mehr als eine Störung im BBS vorhanden ist", RequirementKind.FUNCTIONAL, "23.2");
    req(r, "23.2.3", "Dauerleistung", "Muss für längere Talfahrten ausreichen", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.4", "Ansprechzeit", "Muss im ungünstigsten Achsfall innerhalb von 0,6s geforderte Bremswirkung stellen", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.5", "Standfestigkeit", "Muss gegenüber Alterung und Korrosion Standfest sein", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.6", "Frei von Asbest", "Bremsbeläge müssen frei von Asbest sein", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.7", "abstufbar", "Bremsung muss dosierbar sein.", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.8", "sinnvolle Bremskraftverteilung", "Bremskraftverteilung zwischen Achsen muss in anbetracht der Fahrzeugdynamik sinnvoll gewählt werden", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.9", "Mittlere Vollverzögerung der Betriebsbremse", "ausgekuppelter Motor: >6,43 m/s^2\nbeladen mit ungebremsten Anhänger: >5,4 m/s^2", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.2.10", "Mittlere Vollverzögerung des Hilfsbremssystems", ">2,44 m/s^2", RequirementKind.NON_FUNCTIONAL, "23.2");
    req(r, "23.3", "Stabilität sicherstellen", "Fahrzeug stabilität muss immer sichergestellt sein", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.3.2", "Elektronisches Fahrdynamik-Regelsystem", "Hat das Fahrzeug durch Bremseingriffe zu stabilisieren", RequirementKind.FUNCTIONAL, "23.3");
    req(r, "23.3.2.1", "Fähig an allen Rädern zu wirken", "Muss an jedem Rad wirken können", RequirementKind.FUNCTIONAL, "23.3.2");
    req(r, "23.3.2.2", "Geschwindigkeitsbereich", "(außer wenn ausgeschaltet) über gesamten Geschwindigkeitsbereich >20 km/h aktiv", RequirementKind.NON_FUNCTIONAL, "23.3.2");
    req(r, "23.3.2.3", "Bleibt aktiviert trotz ABS und ASR", "Funktionsfähig trotz ABS und ASR Eingriff", RequirementKind.FUNCTIONAL, "23.3.2");
    req(r, "23.3.3", "Traktionskontrolle", "Kontrolliert den Reifenschlupf", RequirementKind.FUNCTIONAL, "23.3");
    req(r, "23.4", "Störungen erkennen", "Erkennen von Störungen in den verschiedenen Teilsystemen", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.4.1", "Verschleiß erkennen", "Erkennen des Verschleißzustandes der Bremsbeläge", RequirementKind.FUNCTIONAL, "23.4");
    req(r, "23.5", "Fahrzeug feststellen", "Sichern des Fahrzeuges (+ Gespann) gegen wegrollen", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.5.1", "Mittlere Verzögerung der Feststellbremse", "> 1,5 m/s^2", RequirementKind.NON_FUNCTIONAL, "23.5");
    req(r, "23.5.2", "Störungssicherheit bei auslösen", "Darf bei Störung der Elektrik nicht selbstständig wirken", RequirementKind.NON_FUNCTIONAL, "23.5");
    req(r, "23.6", "Kommunikation mit ECU", "Kommunikation mit dem System Fahrzeug", RequirementKind.FUNCTIONAL, "23");
    req(r, "23.6.1", "Störung melden", "Erkannte Störungen an Fahrzeugsteuergerät weitergeben", RequirementKind.FUNCTIONAL, "23.6");
    req(r, "23.7", "Unempfindlichkeit gegenüber externen Einflüssen", "Wirksamkeit darf nicht durch elektrische oder magnetische Felder eingeschränkt werden", RequirementKind.NON_FUNCTIONAL, "23");

    // --- Funktionale Architektur (name, generalisation, allocated requirements)
    fun(f, r, "Räder verzögern", null, "23.2");
    fun(f, r, "Kommunikation mit Steuergerät", null, "23.6", "23.6.1");
    fun(f, r, "Störung erkennen", null, "23.4");
    fun(f, r, "Verschleiß-Überwachung", "Störung erkennen", "23.4.1", "23.6.1");
    fun(f, r, "Assistenzsysteme", null, "23.3");
    fun(f, r, "ESP", "Assistenzsysteme", "23.3.2", "23.3.2.1", "23.3.2.3");
    fun(f, r, "Eingabekraft verstärken", "Räder verzögern", "23.1.1");
    fun(f, r, "ASR", "Assistenzsysteme", "23.3.3");
    fun(f, r, "Wärme abführen", "Räder verzögern");
    fun(f, r, "Partikel abführen", "Räder verzögern");
    fun(f, r, "Feststellbremse-Überwachung", "Störung erkennen", "23.4", "23.6.1");
    fun(f, r, "Störungen melden", "Kommunikation mit Steuergerät", "23.6.1");
    fun(f, r, "Fahrzeug feststellen", null, "23.5");
    fun(f, r, "Eingabe erfassen", "Fahrzeug feststellen", "23.5");
    fun(f, r, "Feststellbremse schalten", "Fahrzeug feststellen", "23.1", "23.5");
    fun(f, r, "ESP-Überwachung", "Störung erkennen", "23.4", "23.6.1");
    fun(f, r, "Bremsdruck-Überwachung", "Störung erkennen", "23.4", "23.6.1");
    fun(f, r, "Bremsdruck umsetzen", "Räder verzögern", "23.2.2");
    fun(f, r, "Bremsassistent", "Eingabekraft verstärken", "23.1.2");
    fun(f, r, "Bremsdruck erzeugen", "Räder verzögern", "23.2.2");
    fun(f, r, "Bremsdruck übertragen", "Räder verzögern", "23.2.2");

    // --- Systemaufbau: structure elements and parts (name, type, kind, owner)
    comp(a, "Bremssystem", null, ComponentKind.STRUCTURE_ELEMENT, null);
    comp(a, "Fahrbremse", null, ComponentKind.STRUCTURE_ELEMENT, "Bremssystem");
    comp(a, "Feststellbremse", null, ComponentKind.STRUCTURE_ELEMENT, "Bremssystem");
    comp(a, "Bremspedal", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Bremskraftverstärker", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Übertragungselemente", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Radbremseinheiten", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Tandemhauptzylinder", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Eingabeeinrichtung", null, ComponentKind.STRUCTURE_ELEMENT, "Feststellbremse");
    comp(a, "Feststell-Mechanismus", null, ComponentKind.STRUCTURE_ELEMENT, "Feststellbremse");
    comp(a, "Bremspedal", "Bremspedal1", ComponentKind.OPERATING_ELEMENT, "Bremspedal");
    comp(a, "Unterdruckpumpe", null, ComponentKind.ACTUATOR, "Bremskraftverstärker");
    comp(a, "Druckstange", null, ComponentKind.MECHANICAL_COMPONENT, "Bremskraftverstärker");
    comp(a, "Druckkammern", null, ComponentKind.MECHANICAL_COMPONENT, "Bremskraftverstärker");
    comp(a, "Bremsleitung / Bremsschlauch", null, ComponentKind.HYDRAULIC_COMPONENT, "Übertragungselemente");
    comp(a, "Bremsscheibe", null, ComponentKind.MECHANICAL_COMPONENT, "Radbremseinheiten");
    comp(a, "Bremsbeläge", null, ComponentKind.MECHANICAL_COMPONENT, "Radbremseinheiten");
    comp(a, "Bremskolben", null, ComponentKind.HYDRAULIC_COMPONENT, "Radbremseinheiten");
    comp(a, "Verschleißsensor", null, ComponentKind.SENSOR, "Radbremseinheiten");
    comp(a, "Tandemhauptzylinder", "Tandemhauptzylinder1", ComponentKind.HYDRAULIC_COMPONENT, "Tandemhauptzylinder");
    comp(a, "Eingabeverarbeitung", null, ComponentKind.CONTROL_UNIT, "Eingabeeinrichtung");
    comp(a, "Aktor", null, ComponentKind.ACTUATOR, "Feststell-Mechanismus");
    comp(a, "Steuergerät", null, ComponentKind.CONTROL_UNIT, "Feststell-Mechanismus");
    comp(a, "Feststellen", null, ComponentKind.MECHANICAL_COMPONENT, "Feststell-Mechanismus");

    // --- «allocate» function -> structure element
    allocate(a, f, "Radbremseinheiten", "Räder verzögern", "Bremsdruck umsetzen", "Partikel abführen",
        "Verschleiß-Überwachung", "Wärme abführen");
    allocate(a, f, "Bremskraftverstärker", "Bremsassistent", "Eingabekraft verstärken");
    allocate(a, f, "Tandemhauptzylinder", "Bremsdruck erzeugen");
    allocate(a, f, "Übertragungselemente", "Bremsdruck übertragen");
    allocate(a, f, "Feststellbremse", "Fahrzeug feststellen", "Feststellbremse-Überwachung",
        "Kommunikation mit Steuergerät", "Störung erkennen", "Störungen melden");
    allocate(a, f, "Feststell-Mechanismus", "Fahrzeug feststellen");
    allocate(a, f, "Eingabeeinrichtung", "Feststellbremse schalten");

    // --- «satisfy» structure element -> requirement
    satisfy(a, r, "Bremssystem", "23.7");
    satisfy(a, r, "Fahrbremse", "23.1", "23.2.2", "23.2.3", "23.2.4", "23.2.5", "23.2.7", "23.2.9", "23.2.10",
        "23.4", "23.6", "23.6.1");
    satisfy(a, r, "Bremskraftverstärker", "23.1.1", "23.1.2");
    satisfy(a, r, "Bremspedal", "23.1.3", "23.2.8");
    satisfy(a, r, "Feststellbremse", "23.4", "23.5", "23.5.2", "23.6", "23.6.1");
    satisfy(a, r, "Feststell-Mechanismus", "23.5.1");
    satisfy(a, r, "Radbremseinheiten", "23.2.1", "23.2.6", "23.4.1");
  }

  // ===========================================================================
  // Scenario 1 (slides 2-5): Gn-1 -> Gn, introduction of ABS
  // ===========================================================================

  /** Step 3: requirement that the brake system shall prevent the wheels from locking (Cameo 23.3.1 ff.). */
  public static void gnStep03AddAntiLockRequirement(CommittableView view) {
    ProductRequirementsRepository r = productRequirements(view, GN);
    req(r, "23.3.1", "Anti-Blockier-System", "Hat das blockieren der Räder zu verhindern um Lenkbarkeit und Stabilität sicherzustellen", RequirementKind.FUNCTIONAL, "23.3");
    req(r, "23.3.1.1", "Abschaltbarkeit", "Darf nicht abgeschaltet werden können", RequirementKind.FUNCTIONAL, "23.3.1");
    req(r, "23.3.1.2", "maximale Regeldauer", "ABS-Regelung muss über einen längeren Zeitraum aufrecht erhalten werden können", RequirementKind.NON_FUNCTIONAL, "23.3.1");
    req(r, "23.3.1.3", "Leistung", "Gilt als ausreichend wenn Kraftschlussausnutzung >75%", RequirementKind.NON_FUNCTIONAL, "23.3.1");
  }

  /** Step 4: requirement to capture the wheel speeds as input of the ABS control. [slide] */
  public static void gnStep04AddWheelSpeedRequirement(CommittableView view) {
    ProductRequirementsRepository r = productRequirements(view, GN);
    req(r, "23.3.1.4", "Raddrehzahlerfassung",
        "Die Raddrehzahlen müssen als Eingangsgröße für die ABS-Regelung erfasst werden", RequirementKind.FUNCTIONAL,
        "23.3.1");
  }

  /** Step 5: function ABS control (Cameo: "ABS", plus its monitoring "ABS-Überwachung"). */
  public static void gnStep05AddAbsFunction(CommittableView view) {
    ProductFunctionsRepository f = productFunctions(view, GN);
    ProductRequirementsRepository r = productRequirements(view, GN);
    fun(f, r, "ABS", "Assistenzsysteme", "23.3", "23.3.1", "23.3.1.1");
    fun(f, r, "ABS-Überwachung", "Störung erkennen", "23.4", "23.6.1");
  }

  /** Step 7: function capturing and providing the wheel-speed information. [slide] */
  public static void gnStep07AddWheelSpeedFunction(CommittableView view) {
    fun(productFunctions(view, GN), productRequirements(view, GN), "Raddrehzahlen erfassen", "ABS", "23.3.1.4");
  }

  /**
   * Step 9: wheel-speed sensors as technical elements (slide 4 also lists the further measured variables
   * acceleration, brake pressure, yaw rate and steering angle). Cameo: parts of the
   * Hydraulisch-Elektrischte-Regeleinheit; the owner is assigned in step 15.
   */
  public static void gnStep09AddSensors(CommittableView view) {
    ProductArchitectureRepository a = productArchitecture(view, GN);
    comp(a, "Raddrehzahlsensoren", null, ComponentKind.SENSOR, null);
    comp(a, "Beschleunigungssensor", null, ComponentKind.SENSOR, null);
    comp(a, "Bremsdrucksensor", null, ComponentKind.SENSOR, null);
    comp(a, "Gierratensensor", null, ComponentKind.SENSOR, null);
    comp(a, "Lenkwinkelsensor", null, ComponentKind.SENSOR, null);
    allocate(a, productFunctions(view, GN), "Raddrehzahlsensoren", "Raddrehzahlen erfassen");
  }

  /** Step 11: ABS control unit (Cameo part "elektrische Regeleinheit"). */
  public static void gnStep11AddAbsControlUnit(CommittableView view) {
    comp(productArchitecture(view, GN), "elektrische Regeleinheit", null, ComponentKind.CONTROL_UNIT, null);
  }

  /**
   * Step 13: hydraulic actuators (valve block, pump) and the adapted hydraulic circuit
   * (Cameo structure element "Hydraulisch-Elektrischte-Regeleinheit", part "steuerventile" of the Fahrbremse).
   */
  public static void gnStep13AddHydraulicComponents(CommittableView view) {
    ProductArchitectureRepository a = productArchitecture(view, GN);
    comp(a, "Hydraulisch-Elektrischte-Regeleinheit", null, ComponentKind.STRUCTURE_ELEMENT, "Fahrbremse");
    comp(a, "Ventilblock", null, ComponentKind.HYDRAULIC_COMPONENT, null);
    comp(a, "Hydraulikpumpe", null, ComponentKind.HYDRAULIC_COMPONENT, null);
  }

  /** Step 15: product-internal relations between the new requirements, functions and components of Gn. */
  public static void gnStep15LinkProductModel(CommittableView view) {
    ProductArchitectureRepository a = productArchitecture(view, GN);
    ProductFunctionsRepository f = productFunctions(view, GN);
    ProductRequirementsRepository r = productRequirements(view, GN);
    ProductComponent controlUnit = component(a, "Hydraulisch-Elektrischte-Regeleinheit");
    for (String part : List.of("Hydraulikpumpe", "Ventilblock", "elektrische Regeleinheit", "Raddrehzahlsensoren",
        "Beschleunigungssensor", "Gierratensensor", "Lenkwinkelsensor", "Bremsdrucksensor")) {
      component(a, part).setParent(controlUnit);
    }
    allocate(a, f, "Hydraulisch-Elektrischte-Regeleinheit", "ABS", "ABS-Überwachung", "ASR", "Assistenzsysteme",
        "Bremsdruck-Überwachung", "ESP", "ESP-Überwachung", "Kommunikation mit Steuergerät", "Störung erkennen",
        "Störungen melden");
    satisfy(a, r, "Hydraulisch-Elektrischte-Regeleinheit", "23.1.2", "23.3.1", "23.3.1.1", "23.3.1.2", "23.3.1.3",
        "23.3.2", "23.3.2.1", "23.3.2.2", "23.3.2.3", "23.3.3");
    satisfy(a, r, "Raddrehzahlsensoren", "23.3.1.4");
  }

  // ===========================================================================
  // Scenario 2 (slides 6-8): Gn -> Gn+1, introduction of recuperative braking
  // ===========================================================================

  /** Step 8 (requirements): recuperation 23.2.11 ff. and brake commands of assistance systems 23.6.2. */
  public static void gnPlus1Step08AddRequirements(CommittableView view) {
    ProductRequirementsRepository r = productRequirements(view, GN_PLUS_1);
    req(r, "23.2.11", "Rekuperation", "Rückeinspeisung der kinetischen Energie des Fahrzeuges in den elektrischen Speicher", RequirementKind.FUNCTIONAL, "23.2");
    req(r, "23.2.11.1", "Bedienung entspricht Kategorie A nach ECE 13-h", "Bedienung erfolgt über die Stellung des Gaspedals (One-Pedal-Driving)", RequirementKind.FUNCTIONAL, "23.2.11");
    req(r, "23.2.11.2", "Anforderungen an Stabilität", "Während Rekuperation gelten die gleichen Anforderungen an die Fahrzeugstabilität wie beim Betriebsbremsen", RequirementKind.FUNCTIONAL, "23.2.11");
    req(r, "23.6.2", "Befehle von Assistenzsystemen erhalten", "Bremsbefehle von Assistenzsystemen erhalten", RequirementKind.FUNCTIONAL, "23.6");
  }

  /** Step 8 (functions): "Rekuperieren" and "Bremsbefehle erhalten". */
  public static void gnPlus1Step08AddFunctions(CommittableView view) {
    ProductFunctionsRepository f = productFunctions(view, GN_PLUS_1);
    ProductRequirementsRepository r = productRequirements(view, GN_PLUS_1);
    fun(f, r, "Rekuperieren", "Räder verzögern", "23.2.11");
    fun(f, r, "Bremsbefehle erhalten", "Kommunikation mit Steuergerät", "23.1", "23.6.2");
  }

  /**
   * Step 8 (architecture): structure element "Rekuperationseinrichtung" (electric machine, battery, inverter,
   * motor controller - slide 7) and the control valve that makes the brake booster externally controllable.
   */
  public static void gnPlus1Step08AddComponents(CommittableView view) {
    ProductArchitectureRepository a = productArchitecture(view, GN_PLUS_1);
    comp(a, "Rekuperationseinrichtung", null, ComponentKind.STRUCTURE_ELEMENT, "Bremssystem");
    comp(a, "Gaspedal", null, ComponentKind.OPERATING_ELEMENT, "Rekuperationseinrichtung");
    comp(a, "Leistungselektronik", null, ComponentKind.POWER_ELECTRONICS, "Rekuperationseinrichtung");
    comp(a, "Generator", null, ComponentKind.ELECTRIC_MACHINE, "Rekuperationseinrichtung");
    comp(a, "Fahrzeugbatterie", null, ComponentKind.ENERGY_STORAGE, "Rekuperationseinrichtung");
    comp(a, "Steuergerät", "Steuergerät1", ComponentKind.CONTROL_UNIT, "Rekuperationseinrichtung");
    comp(a, "Steuerventil", null, ComponentKind.ACTUATOR, "Bremskraftverstärker");
  }

  /** Step 8 (relations): allocations and satisfactions of the new Gn+1 elements. */
  public static void gnPlus1Step08LinkProductModel(CommittableView view) {
    ProductArchitectureRepository a = productArchitecture(view, GN_PLUS_1);
    ProductFunctionsRepository f = productFunctions(view, GN_PLUS_1);
    ProductRequirementsRepository r = productRequirements(view, GN_PLUS_1);
    allocate(a, f, "Rekuperationseinrichtung", "Rekuperieren");
    allocate(a, f, "Eingabeeinrichtung", "Bremsbefehle erhalten");
    allocate(a, f, "Hydraulisch-Elektrischte-Regeleinheit", "Bremsbefehle erhalten");
    satisfy(a, r, "Rekuperationseinrichtung", "23.2.11");
    satisfy(a, r, "Fahrbremse", "23.2.11.2", "23.6.2");
    satisfy(a, r, "Bremspedal", "23.2.11.1");
  }

  // ===========================================================================
  // Builders and lookups
  // ===========================================================================

  public static ProductRequirement req(ProductRequirementsRepository repository, String id, String name, String text,
      RequirementKind kind, String parentId) {
    ProductRequirement requirement = Product_requirementsFactory.eINSTANCE.createProductRequirement();
    requirement.setId(id);
    requirement.setName(name);
    requirement.setText(text);
    requirement.setKind(kind);
    if (parentId != null) {
      requirement.setParent(requirement(repository, parentId));
    }
    repository.getRequirements().add(requirement);
    return requirement;
  }

  public static ProductFunction fun(ProductFunctionsRepository repository, ProductRequirementsRepository requirements,
      String name, String generalName, String... satisfiedRequirementIds) {
    ProductFunction function = Product_functionsFactory.eINSTANCE.createProductFunction();
    function.setName(name);
    if (generalName != null) {
      function.setGeneral(function(repository, generalName));
    }
    for (String id : satisfiedRequirementIds) {
      function.getSatisfies().add(requirement(requirements, id));
    }
    repository.getFunctions().add(function);
    return function;
  }

  public static ProductComponent comp(ProductArchitectureRepository repository, String name, String typeName,
      ComponentKind kind, String parentName) {
    ProductComponent component = Product_architectureFactory.eINSTANCE.createProductComponent();
    component.setName(name);
    component.setTypeName(typeName);
    component.setKind(kind);
    if (parentName != null) {
      component.setParent(structureElement(repository, parentName));
    }
    repository.getComponents().add(component);
    return component;
  }

  private static void allocate(ProductArchitectureRepository architecture, ProductFunctionsRepository functions,
      String structureElementName, String... functionNames) {
    ProductComponent component = component(architecture, structureElementName);
    for (String functionName : functionNames) {
      component.getAllocatedFunctions().add(function(functions, functionName));
    }
  }

  private static void satisfy(ProductArchitectureRepository architecture, ProductRequirementsRepository requirements,
      String structureElementName, String... requirementIds) {
    ProductComponent component = component(architecture, structureElementName);
    for (String id : requirementIds) {
      component.getSatisfies().add(requirement(requirements, id));
    }
  }

  public static ProductRequirement requirement(ProductRequirementsRepository repository, String id) {
    return repository.getRequirements().stream().filter(r -> id.equals(r.getId())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No product requirement " + id));
  }

  public static ProductFunction function(ProductFunctionsRepository repository, String name) {
    return repository.getFunctions().stream().filter(f -> name.equals(f.getName())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No product function " + name));
  }

  /**
   * Component by name. If several components share the name (e.g. the structure element "Bremspedal" and its
   * part "Bremspedal"), the structure element is returned.
   */
  public static ProductComponent component(ProductArchitectureRepository repository, String name) {
    List<ProductComponent> candidates = repository.getComponents().stream()
        .filter(c -> name.equals(c.getName())).toList();
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("No product component " + name);
    }
    return candidates.stream().filter(c -> c.getKind() == ComponentKind.STRUCTURE_ELEMENT).findFirst()
        .orElse(candidates.get(0));
  }

  /** Component identified by owner and name, e.g. ("Rekuperationseinrichtung", "Steuergerät"). */
  public static ProductComponent component(ProductArchitectureRepository repository, String parentName, String name) {
    return repository.getComponents().stream()
        .filter(c -> name.equals(c.getName()))
        .filter(c -> Objects.equals(parentName, c.getParent() == null ? null : c.getParent().getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No product component " + parentName + "::" + name));
  }

  private static ProductComponent structureElement(ProductArchitectureRepository repository, String name) {
    return repository.getComponents().stream()
        .filter(c -> name.equals(c.getName()) && c.getKind() == ComponentKind.STRUCTURE_ELEMENT).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No structure element " + name));
  }

  /** Number of components of a generation (read helper for tests and the example). */
  public static int componentCount(View view, String generation) {
    return productArchitecture(view, generation).getComponents().size();
  }
}
