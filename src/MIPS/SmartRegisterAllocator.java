package MIPS;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartRegisterAllocator {
    private static final int MAX_REGS = 10;
    private final Map<String, String> assignedRegs = new HashMap<>();

    public Map<String, String> allocate(List<String> tacLines) {
        Map<String, List<Integer>> usageLines = new HashMap<>();

        // 1. Detectar on apareix cada tX
        for (int i = 0; i < tacLines.size(); i++) {
            String line = tacLines.get(i);
            Matcher matcher = Pattern.compile("t\\d+").matcher(line);
            while (matcher.find()) {
                String t = matcher.group();
                usageLines.computeIfAbsent(t, k -> new ArrayList<>()).add(i);
            }
        }

        Map<String, String> result = new HashMap<>();
        Map<String, Integer> lastUse = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : usageLines.entrySet()) {
            lastUse.put(entry.getKey(), Collections.max(entry.getValue()));
        }

        Map<String, String> active = new HashMap<>();
        Queue<String> freeRegs = new LinkedList<>();
        for (int i = 0; i < MAX_REGS; i++) freeRegs.add("$t" + i);

        // 2. Assignació dinàmica de registres
        for (int i = 0; i < tacLines.size(); i++) {
            String line = tacLines.get(i);
            Set<String> usedInLine = new HashSet<>();
            Matcher matcher = Pattern.compile("t\\d+").matcher(line);
            while (matcher.find()) {
                String t = matcher.group();
                usedInLine.add(t);

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
