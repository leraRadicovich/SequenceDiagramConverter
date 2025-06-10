import java.io.*;
import java.util.*;
import java.util.regex.*;

import static org.example.PatternStatic.*;

public class getOriginalSyntax {

    public static void main(String[] args) {
        try {
            List<File> inputFiles = UserInputHandler.getInputFiles();
            for (File inputFile : inputFiles) {
                if (isAlreadyProcessed(inputFile)) {
                    System.out.println("Skipping: " + inputFile.getName());
                    continue;
                }
                File outputFile = FileHelper.createOutputFile(inputFile);
                processPumlFile(inputFile, outputFile);
                System.out.println("Processed: " + inputFile.getName());
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void processPumlFile(File inputFile, File outputFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            boolean umlSectionStarted = false;
            boolean headerAdded = false;
            String title = inputFile.getName().replace(".puml", "");
            boolean inSkinparamBlock = false;
            boolean inBox = false;
            Box currentBox = null;
            List<Participant> boxParticipants = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Пропускаем строки с автонумерацией и параметрами стиля
                if (inSkinparamBlock) {
                    if (line.equals("}")) inSkinparamBlock = false;
                    continue;
                } else if (line.startsWith("skinparam")) {
                    if (line.contains("{")) inSkinparamBlock = true;
                    continue;
                } else if (line.startsWith("autonumber")) {
                    continue;
                }

                // Обработка активации
                Matcher activateMatcher = ACTIVATE_PATTERN.matcher(line);
                if (activateMatcher.matches()) {
                    writer.write("ACTIVATE(" + activateMatcher.group(1) + ")\n");
                    continue;
                }

                // Обработка деактивации
                Matcher deactivateMatcher = DEACTIVATE_PATTERN.matcher(line);
                if (deactivateMatcher.matches()) {
                    writer.write("DEACTIVATE(" + deactivateMatcher.group(1) + ")\n");
                    continue;
                }

                // Обработка разделителей
                Matcher dividerMatcher = DIVIDER_PATTERN.matcher(line);
                if (dividerMatcher.matches()) {
                    writer.write("DEVIDER(\"" + dividerMatcher.group(1) + "\")\n");
                    continue;
                }

                // Обработка группировок
                String groupLine = processGroup(line);
                if (groupLine != null) {
                    writer.write(groupLine + "\n");
                    continue;
                }

                // Обработка end
                if (line.equals("end")) {
                    writer.write("END()\n");
                    continue;
                }

                // Обработка box
                Matcher boxMatcher = BOX_START_PATTERN.matcher(line);
                if (boxMatcher.matches()) {
                    inBox = true;
                    currentBox = new Box(boxMatcher.group(1),
                            boxMatcher.group(2) != null ? boxMatcher.group(2) : "");
                    continue;
                }

                if (line.equals("end box")) {
                    writeBoxContent(writer, currentBox, boxParticipants);
                    inBox = false;
                    boxParticipants.clear();
                    continue;
                }

                // Обработка участников (с удалением цвета)
                line = processParticipantColor(line);
                Participant participant = processParticipant(line);
                if (participant != null) {
                    if (inBox) {
                        boxParticipants.add(participant);
                    } else {
                        writer.write(participant.toPartiesString() + "\n");
                    }
                    continue;
                }

                // Обработка стрелок
                String arrowLine = processArrow(line);
                if (arrowLine != null) {
                    writer.write(arrowLine + "\n");
                    continue;
                }

                // Обработка стартовой секции
                if (!umlSectionStarted && line.equals("@startuml")) {
                    umlSectionStarted = true;
                    writer.write("@startuml\n");
                    writer.write("!include C:/Users/Vpatrushev/IdeaProjects/pafp-wiki/UML_LIB/umlLib/seqLib4/SequenceLibIncludeFile_v4.puml\n");
                    writer.write("diagramInit(draft, \"" + title + "\")\n");
                    continue;
                }

                writer.write(line + "\n");
            }
        }
    }

    private static String processGroup(String line) {
        Matcher matcher = GROUP_PATTERN.matcher(line);
        if (!matcher.matches()) return null;

        String type = matcher.group(1).toUpperCase();
        String rest = matcher.group(2).trim();

        String color = "";
        String text = rest;

        Matcher colorMatcher = GROUP_COLOR_PATTERN.matcher(rest);
        if (colorMatcher.find()) {
            color = colorMatcher.group(1) != null ?
                    colorMatcher.group(1).replace("#", "") :
                    colorMatcher.group(2);

            text = rest.substring(colorMatcher.end()).trim();
        }

        return String.format("%s(%s, \"%s\")",
                type,
                color,
                text.replace("\"", "\\\""));
    }

    private static String processParticipantColor(String line) {
        return PARTICIPANT_COLOR_PATTERN.matcher(line).replaceAll("");
    }

    private static Participant processParticipant(String line) {
        Matcher matcher = PARTICIPANT_PATTERN.matcher(line);
        if (!matcher.matches()) return null;

        return new Participant(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                matcher.group(4)
        );
    }

    private static String processArrow(String line) {
        Matcher matcher = ARROW_PATTERN.matcher(line);
        if (!matcher.matches()) return null;

        String left = matcher.group(1);
        String arrow = matcher.group(2);
        String right = matcher.group(3);
        String operators = matcher.group(4);
        String text = matcher.group(5).replaceAll("\"", "");

        boolean isReverse = arrow.startsWith("<");
        String procType = arrow.contains("--") ? "rs" : "rq";
        String from = isReverse ? right : left;
        String to = isReverse ? left : right;

        return String.format("%s(%s, %s, \"%s\", \"%s\", \"\")",
                procType, from, to, operators, text);
    }

    private static void writeBoxContent(BufferedWriter writer, Box box, List<Participant> participants) throws IOException {
        for (Participant p : participants) {
            writer.write(p.toPartiesString() + "\n");
        }

        StringBuilder participantsList = new StringBuilder();
        for (Participant p : participants) {
            participantsList.append(p.alias != null ? p.alias : p.name).append(",");
        }
        if (participantsList.length() > 0) {
            participantsList.deleteCharAt(participantsList.length() - 1);
        }

        writer.write(String.format("BOX(\"%s\", %s, \"%s\")\n",
                box.name, box.color, participantsList));
    }

    private static boolean isAlreadyProcessed(File file) {
        return file.getName().toLowerCase().endsWith("_bylib.puml");
    }

    static class Participant {
        String type;
        String name;
        String alias;
        String order;

        Participant(String type, String name, String alias, String order) {
            this.type = type;
            this.name = name;
            this.alias = alias;
            this.order = order;
        }

        String toPartiesString() {
            return String.format("parties(%s,\"%s\",%s,%s)",
                    type, name, alias != null ? alias : "", order != null ? order : "");
        }
    }

    static class Box {
        String name;
        String color;

        Box(String name, String color) {
            this.name = name;
            this.color = color != null ? color : "";
        }
    }

    static class UserInputHandler {
        public static List<File> getInputFiles() throws IOException {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter path: ");
            File path = new File(scanner.nextLine().trim());

            List<File> files = new ArrayList<>();

            if (path.isDirectory()) {
                File[] found = path.listFiles(f ->
                        f.getName().endsWith(".puml") && !isAlreadyProcessed(f));
                if (found != null) Collections.addAll(files, found);
            } else if (path.isFile() && !isAlreadyProcessed(path)) {
                files.add(path);
            }

            if (files.isEmpty()) throw new IOException("No files to process");
            return files;
        }

        private static boolean isAlreadyProcessed(File f) {
            return f.getName().toLowerCase().endsWith("_bylib.puml");
        }
    }

    static class FileHelper {
        public static File createOutputFile(File input) {
            String name = input.getName().replace(".puml", "_byLib.puml");
            return new File(input.getParentFile(), name);
        }
    }
}