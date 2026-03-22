# Checklists for testing

## Module: Teacher Schedule Viewing (Public Area)

### Positive Scenarios (Functionality)
- [ ] Verify successful schedule display when a valid semester and teacher are selected (with existing classes in the database).
- [ ] Verify the correct display of the "Schedule is empty" message for a teacher with no scheduled classes in the selected semester.
- [ ] Verify the dependency of dropdown lists: when selecting a specific "Department", only teachers from that department remain in the "Teacher" list.
- [ ] Verify the schedule display in the "All" format (general schedule for the semester).
- [ ] Verify the schedule display when changing the value in the "Format" field (e.g., selecting a specific week).
- [ ] Verify the accuracy of the data in the generated table (matching days of the week, time, subject name, and room with the database data).
- [ ] Verify that the search (play) button becomes active only after the mandatory fields (Semester and Teacher) are selected.

### Negative Scenarios
- [ ] Verify the inability to search (the button remains disabled) if a teacher is selected, but a semester is not.
- [ ] Verify the inability to search (the button remains disabled) if a semester is selected, but a teacher is not.
- [ ] Verify system behavior when attempting to spoof the teacher ID via developer tools (DevTools) to a non-existent one (e.g., `9999`) before sending the request (expecting a handled "Not found" error, not a site crash).
- [ ] Verify system behavior when attempting to spoof the semester ID via DevTools to a text value (e.g., `abc`) (expecting a Validation Error / Bad Request).
- [ ] Verify page loading and filter behavior when simulating a loss of internet connection (Offline mode).

### UI/UX
- [ ] Verify the adaptability of the page and the generated table on mobile devices (ensure there is no horizontal scrolling that breaks the layout).
- [ ] Verify text readability, color contrast, and spacing within the generated schedule table.
- [ ] Verify the presence and clarity of the text instruction for the user at the top of the page.
- [ ] Verify the correct operation of the language switcher (UK/UA flags) — instant translation of all interface elements, placeholders, and messages (e.g., "Schedule is empty").
- [ ] Verify the presence of visual feedback (hover effects) when the cursor hovers over the search button, dropdown fields, and navigation buttons ("Home", "Log in").
