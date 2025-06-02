package org.example;

import java.util.regex.Pattern;

public interface PatternStatic {
     static final Pattern PARTICIPANT_PATTERN = Pattern.compile(
            "^(actor|participant|database|queue)\\s+\"([^\"]+)\"(?:\\s+as\\s+(\\w+))?(?:\\s+order\\s+(\\d+))?.*"
    );

     static final Pattern BOX_START_PATTERN = Pattern.compile(
            "^box\\s+\"([^\"]+)\"(?:\\s+#(\\w+))?.*"
    );

     static final Pattern ARROW_PATTERN = Pattern.compile(
            "^\\s*([a-zA-Z0-9_]+)\\s*([<]?--?[>]?)\\s*([a-zA-Z0-9_]+)\\s*([+\\-]*)\\s*:\\s*(.+?)\\s*$"
    );

     static final Pattern GROUP_PATTERN = Pattern.compile(
            "^(alt|opt|par|group|else)\\b\\s*(.*?)\\s*$"
    );

     static final Pattern GROUP_COLOR_PATTERN = Pattern.compile(
            "(#\\w+)|%\\w+\\((\\w+),\\s*-?\\d+\\)"
    );

     static final Pattern PARTICIPANT_COLOR_PATTERN = Pattern.compile(
            "\\s*#\\w+\\s*$"
    );

    // Новые паттерны для активации/деактивации и разделителей
     static final Pattern ACTIVATE_PATTERN = Pattern.compile(
            "^(?:activate\\s+)?(\\w+)\\s*\\+\\+\\s*$"
    );

     static final Pattern DEACTIVATE_PATTERN = Pattern.compile(
            "^(?:deactivate\\s+)?(\\w+)\\s*--\\s*$"
    );

     static final Pattern DIVIDER_PATTERN = Pattern.compile(
            "^==\\s*(.+?)\\s*==\\s*$"
    );
}
