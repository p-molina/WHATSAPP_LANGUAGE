package MIPS;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assingnador de registres intel·ligent per a TAC (Three-Address Code).
 * Utilitza un algoritme lineal basat en l'última utilització de cada variable temporal (tX)
 * per assignar registres de manera eficient.
 */
public class SmartRegisterAllocator {
    /** Nombre màxim de registres temporals disponibles (t0–t9). */
    private static final int MAX_REGS = 10;
    /** Mapa de registres assignats: clau = variable TAC (tX), valor = registre MIPS ($tX). */
    private final Map<String, String> assignedRegs = new HashMap<>();

    /**
     * Assigna un registre a cada variable temporal tX present al TAC.
     *
     * L'algorisme segueix dos passos principals:
     * 1. Detecta totes les aparicions de cada tX i determina la seva última línia d'ús.
     * 2. Recorre el TAC i, per cada aparició de tX, asigna un registre lliure si encara no en té,
     *    i allibera registres quan la variable ja no serà més utilitzada.
     *
     * @param tacLines Llista de línies de codi TAC a processar.
     * @return Map que associa cadascuna de les variables tX amb el registre assignat.
     */
    public Map<String, String> allocate(List<String> tacLines) {
        // Recollir totes les línies on apareix cada tX
        Map<String, List<Integer>> usageLines = new HashMap<>();
        for (int i = 0; i < tacLines.size(); i++) {
            String line = tacLines.get(i);
            Matcher matcher = Pattern.compile("t\\d+").matcher(line);
            while (matcher.find()) {
                String t = matcher.group();
                usageLines.computeIfAbsent(t, k -> new ArrayList<>()).add(i);
            }
        }

        // Preparar mapes per al resultat i per emmagatzemar l'última línia d'ús
        Map<String, String> result = new HashMap<>();
        Map<String, Integer> lastUse = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : usageLines.entrySet()) {
            // Última línia d'ús de tX
            lastUse.put(entry.getKey(), Collections.max(entry.getValue()));
        }

        // Mapa de registres actius i cua de registres disponibles
        Map<String, String> active = new HashMap<>();
        Queue<String> freeRegs = new LinkedList<>();
        for (int i = 0; i < MAX_REGS; i++) freeRegs.add("$t" + i);

        // Assignació dinàmica de registres
        for (int i = 0; i < tacLines.size(); i++) {
            String line = tacLines.get(i);
            Set<String> usedInLine = new HashSet<>();
            Matcher matcher = Pattern.compile("t\\d+").matcher(line);
            while (matcher.find()) {
                String t = matcher.group();
                usedInLine.add(t);
                // Si tX encara no té registre assignat, n'agafem un de lliure
                if (!active.containsKey(t)) {
                    if (freeRegs.isEmpty())
                        throw new RuntimeException("Massa registres vius alhora (línia " + i + ")");
                    String reg = freeRegs.poll();
                    active.put(t, reg);
                    result.put(t, reg);
                }
            }
            // Alliberar registres que ja no són vius
            Iterator<Map.Entry<String, String>> it = active.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String t = entry.getKey();
                if (lastUse.get(t) == i) {
                    freeRegs.add(entry.getValue());
                    it.remove();
                }
            }
        }
        return result;
    }
}
