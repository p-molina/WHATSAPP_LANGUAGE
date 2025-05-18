# Llenguatge WhatsApp

---

## Objectiu del projecte

L’objectiu d’aquest projecte és dissenyar i implementar un llenguatge de programació propi, anomenat **WhatsappLang**, juntament amb el seu compilador complet. El llenguatge s’inspira en la sintaxi informal de les converses a WhatsApp, amb l’objectiu de crear una sintaxi accessible i fàcil d’aprendre per a un públic jove o novell.

El compilador desenvolupat cobreix totes les fases clàssiques: anàlisi lèxica, anàlisi sintàctica, anàlisi semàntica, generació de codi intermedi (TAC) i traducció a MIPS.

---

## Estructura del projecte

El projecte està organitzat per capes modulars dins la carpeta `src`, seguint l’estructura clàssica d’un compilador. També es fa ús de fitxers de recursos (gramàtica i diccionari) i d’una estructura de tests automatitzats.

```
WHATSAPP_LANGUAGE/
│
├── src/
│   ├── entities/                   
│   │   ├── Token.java                      # Representació dels tokens del llenguatge
│   │   ├── Symbol.java                     # Entrada a la taula de símbols
│   │   ├── SymbolTable.java                # Gestió de declaracions i escopes
│   │   ├── Node.java                       # Node de l’arbre sintàctic
│   │   ├── Grammar.java                    # Lectura de la gramàtica des de JSON
│   │   ├── ParserTable.java                # Taula de parsing
│   │   └── Dictionary.java                 # Diccionari de paraules reservades
│   |
│   ├── LexicalAnalyzer/
│   │   └── LexicalAnalyzer.java            # Anàlisi lèxica i generació de tokens
│   |
│   ├── ParserAnalyzer/
│   │   ├── ParserAnalyzer.java             # Generador de l’arbre sintàctic
│   │   ├── ParserTableGenerator.java       # Generació de la taula de parsing
│   │   ├── FirstFollow.java                # Càlcul dels conjunts FIRST/FOLLOW
│   │   └── GramaticalErrorType.java        # Tipus d’errors sintàctics
│   |
│   ├── SemanticAnalyzer/
│   │   ├── SemanticAnalyzer.java           # Validació semàntica
│   │   └── SemanticErrorType.java          # Tipus d’errors semàntics
│   |
│   ├── TAC/
│   │   └── TACGenerator.java               # Generació de codi intermedi (TAC)
│   |
│   ├── MIPS/
│   │   ├── MIPSGenerator.java              # Traducció de TAC a MIPS
│   │   └── SmartRegisterAllocator.java     # Gestió de registres
│   |
│   ├── Testing/
│   │   ├── Test.java                       # Definició de proves
│   │   └── TestExecute.java                # Execució automatitzada de testos
│   |
│   └── Main.java                           # Punt d’entrada principal del compilador
│
├── resources/
│   ├── files/
│   │   ├── wsp/                            # Fitxers font en WhatsappLang
│   │   ├── tac/                            # Codi TAC generat
│   │   └── mips/                           # Codi MIPS generat
│   └── tests/
│       ├── diccionari.json                 # Diccionari de paraules reservades
│       └── grammar.json                    # Definició formal de la gramàtica
│
├── lib/
│   └── json-simple-1.1.1.jar               # Llibreria per gestionar fitxers JSON
├── README.md                               # Aquest document
```

---

## Desenvolupament

El projecte ha estat desenvolupat íntegrament mitjançant l’entorn de desenvolupament **IntelliJ IDEA**, que facilita la gestió modular del projecte, la compilació i l’execució, així com la integració de llibreries externes i la configuració de testos automatitzats.

---

## Configuració de l’entorn

### Configuració del SDK

1. Ves a `File > Project Structure`.
2. Afegeix un nou SDK: `New > Add JDK`.
3. Selecciona el directori on tens instal·lat el JDK (Java 8 o superior).
4. Assigna l’SDK al projecte i aplica els canvis.

### Configuració de les carpetes

1. A `Project Structure > Modules > Sources`:
2. Marca `src/` com a `Sources Root`.
3. Marca `resources/` com a `Resources Root`.
4. Aplica els canvis.

### Importació del `.jar`

1. Ves a `Project Structure > Libraries`.
2. Afegeix una nova llibreria (`+ > Java`).
3. Selecciona `lib/json-simple-1.1.1.jar`.
4. Assigna-la al mòdul i aplica els canvis.

---

## Com executar-lo

1. Compila i executa la classe `Main.java`.
2. Introdueix el nom (sense extensió) d’un fitxer `.wsp` dins `resources/files/wsp`.
3. El compilador realitzarà totes les fases i generarà:
    - Codi TAC a `resources/files/tac/`.
    - Codi MIPS a `resources/files/mips/`.
4. Es mostrarà informació del procés per pantalla.

---

## Execució de tests

Per executar la versió de proves:

1. Executa la classe `TestExecute.java`.
2. Afegeix l’argument `-test` a la configuració d’execució.
3. El sistema validarà automàticament els fitxers dins `resources/tests/`.

---

## Exemple de codi

Exemple d’una funció recursiva per calcular la sèrie de Fibonacci:

```
num n -> 10 xd
num resultat -> 0 xd
num res1 -> 0 xd
num res2 -> 0 xd
num resposta -> 0 xd

num fibonacci jajaj
    bro ¿ n < 2 ? jajaj
        resultat -> n xd
    jejej
    sino jajaj
        n -> n - 1 xd
        res1 -> fibonacci xd

        n -> n - 1 xd
        res2 -> fibonacci xd

        resultat -> res1 + res2 xd
    jejej

    xinpum resultat xd
jejej

num xat jajaj
    resposta -> fibonacci xd
    xinpum resposta xd
jejej
```
---

## Integrants

- Nil Hernández Jiménez — `nil.hernandez@students.salle.url.edu`
- Paula Fernández Lago — `paula.fernandez@students.salle.url.edu`
- Jan Piñol Castuera — `jan.pinol@students.salle.url.edu`
- Pablo Molina Bengochea — `p.molina@students.salle.url.edu`
- Oriol Guimó Morell — `oriol.guimo@students.salle.url.edu`

---