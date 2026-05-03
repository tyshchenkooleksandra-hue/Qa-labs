package com.softserve.util;

import com.softserve.dto.*;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds styled HTML for a teacher schedule using rowspan-based layout.
 *
 * <p>Key differences from group schedule:</p>
 * <ul>
 *   <li>Only days that have at least one lesson are shown as columns</li>
 *   <li>Only periods from the earliest occupied to the latest occupied are shown as rows</li>
 *   <li>Even/odd weeks are at the day level (not lesson level)</li>
 *   <li>Each period can have multiple lessons (teacher teaches several groups)</li>
 *   <li>Cards show group name instead of teacher name</li>
 * </ul>
 */
@Slf4j
public class TeacherHtmlBuilder {

    public String buildHtml(ScheduleForTeacherDTO schedule, Locale language) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", language);

        Map<DayOfWeek, DaysOfWeekWithClassesForTeacherDTO> dayMap = new LinkedHashMap<>();
        if (schedule.getDays() != null) {
            for (DaysOfWeekWithClassesForTeacherDTO day : schedule.getDays()) {
                dayMap.put(day.getDay(), day);
            }
        }

        // Only days that have at least one lesson
        List<DayOfWeek> activeDays = getActiveDays(dayMap);

        // All periods, then trim to min–max occupied range
        List<PeriodDTO> allPeriods = collectSortedPeriods(schedule);
        List<PeriodDTO> periods = trimPeriodsToOccupied(allPeriods, dayMap);

        TeacherDTO teacher = schedule.getTeacher();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"").append(language.getLanguage()).append("\">");
        html.append("<head><meta charset=\"UTF-8\"/>");
        html.append("<style>").append(SchedulePdfStyles.get()).append("</style>");
        html.append("</head><body>");

        // Header: "Розклад для ПІБ, посада (семестр)"
        html.append("<div class=\"header__title\">");
        html.append(esc(bundle.getString("schedule.group.for")))
                .append(" ")
                .append(esc(formatTeacherFull(teacher)));
        if (teacher != null && teacher.getPosition() != null && !teacher.getPosition().isBlank()) {
            html.append(", ").append(esc(teacher.getPosition()));
        }
        if (schedule.getSemester() != null && schedule.getSemester().getDescription() != null
                && !schedule.getSemester().getDescription().isBlank()) {
            html.append(" (").append(esc(schedule.getSemester().getDescription())).append(")");
        }
        html.append("</div>");

        // Legend
        html.append(SchedulePdfStyles.legendHtml(bundle));

        // Table
        html.append("<table class=\"schedule\">");
        html.append("<colgroup><col class=\"col-time\"/>");
        for (int i = 0; i < activeDays.size(); i++) html.append("<col/>");
        html.append("</colgroup>");

        html.append("<thead><tr>");
        html.append("<th>").append(esc(bundle.getString("schedule.pair"))).append("</th>");
        for (DayOfWeek day : activeDays) {
            html.append("<th>").append(esc(getDayName(day, language))).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        // Each period = 2 rows (even + odd)
        for (PeriodDTO period : periods) {
            // Row 1: even week
            html.append("<tr class=\"even\">");
            html.append(SchedulePdfStyles.timeCellHtml(period));

            for (DayOfWeek dayOfWeek : activeDays) {
                html.append("<td>");
                DaysOfWeekWithClassesForTeacherDTO dayData = dayMap.get(dayOfWeek);
                List<LessonForTeacherScheduleDTO> lessons = findLessonsForPeriod(
                        dayData != null ? dayData.getEvenWeek() : null, period);
                html.append(renderLessons(lessons, bundle));
                html.append("</td>");
            }
            html.append("</tr>");

            // Row 2: odd week
            html.append("<tr class=\"odd\">");
            for (DayOfWeek dayOfWeek : activeDays) {
                html.append("<td>");
                DaysOfWeekWithClassesForTeacherDTO dayData = dayMap.get(dayOfWeek);
                List<LessonForTeacherScheduleDTO> lessons = findLessonsForPeriod(
                        dayData != null ? dayData.getOddWeek() : null, period);
                html.append(renderLessons(lessons, bundle));
                html.append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table></body></html>");
        return html.toString();
    }

    // ==================== Rendering ====================

    /**
     * Merges multiple lessons for the same period into a single card.
     * Groups are listed comma-separated. If subjects differ, the most frequent one is used.
     */
    private String renderLessons(List<LessonForTeacherScheduleDTO> lessons, ResourceBundle bundle) {
        if (lessons.isEmpty()) {
            return SchedulePdfStyles.emptyHtml();
        }

        // Pick subject: most frequent, or first if tie
        String subject = lessons.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getSubjectForSite() != null ? l.getSubjectForSite() : "",
                        Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue)
                        .thenComparing(e -> -lessons.indexOf(
                                lessons.stream()
                                        .filter(l -> e.getKey().equals(l.getSubjectForSite()))
                                        .findFirst().orElse(null))))
                .map(Map.Entry::getKey)
                .orElse("");

        // Pick lessonType from first lesson with the chosen subject, or just first
        LessonForTeacherScheduleDTO representative = lessons.stream()
                .filter(l -> subject.equals(l.getSubjectForSite()))
                .findFirst()
                .orElse(lessons.get(0));

        // Collect all group names
        String groups = lessons.stream()
                .filter(l -> l.getGroup() != null && l.getGroup().getTitle() != null)
                .map(l -> l.getGroup().getTitle())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));

        // Room from representative
        String room = representative.getRoom();

        String cssType = SchedulePdfStyles.mapLessonTypeToCss(
                representative.getLessonType() != null ? representative.getLessonType().name() : null);
        String typeName = SchedulePdfStyles.mapLessonTypeToDisplay(
                representative.getLessonType() != null ? representative.getLessonType().name() : null, bundle);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card card--").append(cssType).append("\">");
        sb.append("<div class=\"card__type\">").append(esc(typeName)).append("</div>");
        sb.append("<div class=\"card__subject\">").append(esc(subject)).append("</div>");
        if (!groups.isEmpty()) {
            sb.append("<div class=\"card__info\">").append(esc(groups)).append("</div>");
        }
        if (room != null && !room.isBlank()) {
            sb.append("<div class=\"card__info\">").append(esc(room)).append("</div>");
        }
        // Link from representative lesson
        String link = representative.getLinkToMeeting();
        if (link != null && !link.isBlank()) {
            sb.append("<div class=\"card__link\"><a href=\"")
                    .append(esc(link))
                    .append("\">").append(esc(bundle.getString("schedule.link")))
                    .append("</a></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // ==================== Active days (only with lessons) ====================

    private List<DayOfWeek> getActiveDays(Map<DayOfWeek, DaysOfWeekWithClassesForTeacherDTO> dayMap) {
        List<DayOfWeek> allDays = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        );
        return allDays.stream()
                .filter(d -> {
                    DaysOfWeekWithClassesForTeacherDTO data = dayMap.get(d);
                    if (data == null) return false;
                    return hasLessons(data.getEvenWeek()) || hasLessons(data.getOddWeek());
                })
                .collect(Collectors.toList());
    }

    private boolean hasLessons(ClassesInScheduleForTeacherDTO week) {
        if (week == null || week.getPeriods() == null) return false;
        return week.getPeriods().stream()
                .anyMatch(c -> c.getLessons() != null && c.getLessons().stream()
                        .anyMatch(l -> l.getSubjectForSite() != null && !l.getSubjectForSite().isBlank()));
    }

    // ==================== Period range (min to max occupied) ====================

    /**
     * Trims the full list of periods to only those from the earliest occupied
     * to the latest occupied (inclusive), keeping empty ones in between.
     */
    private List<PeriodDTO> trimPeriodsToOccupied(List<PeriodDTO> allPeriods,
                                                  Map<DayOfWeek, DaysOfWeekWithClassesForTeacherDTO> dayMap) {
        if (allPeriods.isEmpty()) return allPeriods;

        Set<Long> occupiedPeriodIds = new HashSet<>();
        for (DaysOfWeekWithClassesForTeacherDTO day : dayMap.values()) {
            collectOccupiedPeriodIds(day.getEvenWeek(), occupiedPeriodIds);
            collectOccupiedPeriodIds(day.getOddWeek(), occupiedPeriodIds);
        }

        // Find first and last occupied index
        int first = -1, last = -1;
        for (int i = 0; i < allPeriods.size(); i++) {
            if (occupiedPeriodIds.contains(allPeriods.get(i).getId())) {
                if (first == -1) first = i;
                last = i;
            }
        }

        if (first == -1) return allPeriods; // no lessons at all, show all
        return allPeriods.subList(first, last + 1);
    }

    private void collectOccupiedPeriodIds(ClassesInScheduleForTeacherDTO week, Set<Long> ids) {
        if (week == null || week.getPeriods() == null) return;
        for (ClassForTeacherScheduleDTO cls : week.getPeriods()) {
            if (cls.getPeriod() != null && cls.getLessons() != null) {
                boolean hasLesson = cls.getLessons().stream()
                        .anyMatch(l -> l.getSubjectForSite() != null && !l.getSubjectForSite().isBlank());
                if (hasLesson) {
                    ids.add(cls.getPeriod().getId());
                }
            }
        }
    }

    // ==================== Helpers ====================

    private List<LessonForTeacherScheduleDTO> findLessonsForPeriod(
            ClassesInScheduleForTeacherDTO weekData, PeriodDTO period) {
        if (weekData == null || weekData.getPeriods() == null) {
            return Collections.emptyList();
        }
        return weekData.getPeriods().stream()
                .filter(c -> c.getPeriod() != null && c.getPeriod().getId().equals(period.getId()))
                .flatMap(c -> c.getLessons() != null ? c.getLessons().stream() : java.util.stream.Stream.empty())
                .filter(l -> l.getSubjectForSite() != null && !l.getSubjectForSite().isBlank())
                .collect(Collectors.toList());
    }

    private List<PeriodDTO> collectSortedPeriods(ScheduleForTeacherDTO schedule) {
        Map<Long, PeriodDTO> map = new LinkedHashMap<>();
        if (schedule.getDays() != null) {
            for (DaysOfWeekWithClassesForTeacherDTO day : schedule.getDays()) {
                collectPeriodsFromWeek(day.getEvenWeek(), map);
                collectPeriodsFromWeek(day.getOddWeek(), map);
            }
        }
        return map.values().stream()
                .sorted(Comparator.comparing(PeriodDTO::getStartTime))
                .collect(Collectors.toList());
    }

    private void collectPeriodsFromWeek(ClassesInScheduleForTeacherDTO week, Map<Long, PeriodDTO> map) {
        if (week == null || week.getPeriods() == null) return;
        for (ClassForTeacherScheduleDTO cls : week.getPeriods()) {
            if (cls.getPeriod() != null) {
                map.putIfAbsent(cls.getPeriod().getId(), cls.getPeriod());
            }
        }
    }

    private String formatTeacherFull(TeacherDTO t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        if (t.getSurname() != null) sb.append(t.getSurname());
        if (t.getName() != null) sb.append(" ").append(t.getName());
        if (t.getPatronymic() != null) sb.append(" ").append(t.getPatronymic());
        return sb.toString().trim();
    }

    // ==================== Day names ====================

    private static final Map<DayOfWeek, String> UK_DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Понеділок",
            DayOfWeek.TUESDAY, "Вівторок",
            DayOfWeek.WEDNESDAY, "Середа",
            DayOfWeek.THURSDAY, "Четвер",
            DayOfWeek.FRIDAY, "П'ятниця",
            DayOfWeek.SATURDAY, "Субота"
    );

    private static final Map<DayOfWeek, String> EN_DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Monday",
            DayOfWeek.TUESDAY, "Tuesday",
            DayOfWeek.WEDNESDAY, "Wednesday",
            DayOfWeek.THURSDAY, "Thursday",
            DayOfWeek.FRIDAY, "Friday",
            DayOfWeek.SATURDAY, "Saturday"
    );

    private static String getDayName(DayOfWeek day, Locale locale) {
        if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
            return EN_DAY_NAMES.getOrDefault(day, day.name());
        }
        return UK_DAY_NAMES.getOrDefault(day, day.name());
    }

    private static String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
