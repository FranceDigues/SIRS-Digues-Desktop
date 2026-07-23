/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 * 
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.ui.calendar;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import javafx.geometry.VPos;
import javafx.scene.layout.RowConstraints;

/**
 * Responsible for displaying the days of a month.
 *
 * @author Christian Schudt
 */
final class MonthView extends DatePane {

    private static final String CSS_CALENDAR_MONTH_VIEW = "calendar-month-view";
    private static final String CSS_CALENDAR_DAY = "calendar-cell";
    private static final String CSS_CALENDAR_DAY_HAS_EVENTS = "calendar-cell-has-events";
    private static final String CSS_CALENDAR_DAY_CURRENT_MONTH = "calendar-cell-current-month";
    private static final String CSS_CALENDAR_DAY_OTHER_MONTH = "calendar-cell-other-month";
    private static final String CSS_CALENDAR_TODAY = "calendar-cell-today";
    private static final String CSS_CALENDAR_SELECTED = "calendar-cell-selected";
    private static final String CSS_CALENDAR_WEEKDAYS = "calendar-weekdays";
    private static final String CSS_CALENDAR_WEEK_NUMBER = "calendar-week-number";

    /**
     * The number of days per week.
     * I don't know if there is a culture with more or less than seven days per week, but theoretically {@link java.util.Calendar} allows it.
     * This variable will correspond to the number of columns.
     */
    private int numberOfDaysPerWeek;

    /**
     * Constructs the month view.
     *
     * @param calendarView The calendar view.
     */
    public MonthView(final CalendarView calendarView) {
        super(calendarView);

        getStyleClass().add(CSS_CALENDAR_MONTH_VIEW);

        // When the locale changed, update the weeks to the new locale.
        calendarView.localeProperty().addListener(observable -> {
            updateContent();
        });

        // When the disabled week days change, update the days.
        calendarView.getDisabledWeekdays().addListener((Observable observable) -> {
            updateDays();
        });

        // When the disabled dates change, update the days.
        calendarView.getDisabledDates().addListener((Observable observable) -> {
            updateDays();
        });

        // When the disabled dates change, update the days.
        calendarView.showWeeksProperty().addListener(observable -> {
            getChildren().clear();
            buildContent();
            updateContent();
        });
        
        calendarView.selectedDateProperty().addListener(observable -> {
            updateContent();
        });
        
        calendarView.calendarDate.addListener(observable -> {
            updateContent();
        });

        final ObservableList<CalendarEvent> events = calendarView.getCalendarEvents();
        events.addListener((ListChangeListener<CalendarEvent>) c -> updateDays());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildContent() {
        Calendar calendar = calendarView.calendarProperty().get();

        // get the maximum number of days in a week for this calendar.
        numberOfDaysPerWeek = calendar.getMaximum(Calendar.DAY_OF_WEEK);

        // get the maximum number of days a month could have.
        int maxNumberOfDaysInMonth = calendar.getMaximum(Calendar.DAY_OF_MONTH);

        // assume the first row has only 1 day, then distribute the rest among the remaining weeks and add the first week.
        int numberOfRows = (int) Math.ceil((maxNumberOfDaysInMonth - 1) / (double) numberOfDaysPerWeek) + 1;

        // remove all controls
        getChildren().clear();

        int colOffset = calendarView.getShowWeeks() ? 1 : 0;

        if (calendarView.getShowWeeks()) {
            Label empty = new Label();
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.getStyleClass().add(CSS_CALENDAR_WEEKDAYS);
            add(empty, 0, 0);
        }
        // iterate through the columns
        for (int i = 0; i < numberOfDaysPerWeek; i++) {
            Label label = new Label();
            label.getStyleClass().add(CSS_CALENDAR_WEEKDAYS);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER_LEFT);
            add(label, i + colOffset, 0);
        }

        // iterate through the rows
        for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {

            if (calendarView.getShowWeeks()) {
                Label label = new Label();
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMaxHeight(Double.MAX_VALUE);
                label.getStyleClass().add(CSS_CALENDAR_WEEK_NUMBER);
                add(label, 0, rowIndex + 1);
            }

            // iterate through the columns
            for (int colIndex = 0; colIndex < numberOfDaysPerWeek; colIndex++) {
                final GridPane grid = new GridPane();
                setVgrow(grid, Priority.ALWAYS);
                setHgrow(grid, Priority.ALWAYS);
                
                // Prepare buttons style
                grid.setMinSize(0, 0);
                grid.setMaxWidth(Double.MAX_VALUE);
                // HACK -redmine 7611 : Allow to get even row height even
                // when many events are on a date.
                // The scrollPane contained in the grid will allow to see all events.
                grid.maxHeightProperty().bind(this.heightProperty().divide(numberOfRows));

                grid.getStyleClass().add(CSS_CALENDAR_DAY);
                final ColumnConstraints eventColCstr = new ColumnConstraints(0, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true);
                final ColumnConstraints labelColCstr = new ColumnConstraints(USE_PREF_SIZE, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.RIGHT, true);
                final RowConstraints dayRowConstraints = new RowConstraints(20, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.SOMETIMES, VPos.TOP, false);
                final RowConstraints eventRowCstr = new RowConstraints(20, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, VPos.TOP, true);
                grid.getColumnConstraints().addAll(eventColCstr, labelColCstr);
                grid.getRowConstraints().addAll(dayRowConstraints, eventRowCstr);

                //button.setOnAction(actionEvent -> calendarView.selectedDate.set((Date) button.getUserData()));
                // add the button, starting at second row.
                add(grid, colIndex + colOffset, rowIndex + 1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateContent() {
        updateDays();
        updateWeekNames();
    }

    /**
     * Updates the days.
     */
    private void updateDays() {
        Calendar calendar = calendarView.getCalendar();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, calendarView.localeProperty().get());

        dateFormat.setCalendar(calendarView.getCalendar());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);


        // Set the calendar to the first day of the current month.
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int month = calendar.get(Calendar.MONTH);

        Date tmp = calendar.getTime();

        calendar.setTime(new Date());
        int todayDay = calendar.get(Calendar.DATE);
        int todayMonth = calendar.get(Calendar.MONTH);
        int todayYear = calendar.get(Calendar.YEAR);
        
        if (calendarView.selectedDate.get() != null) {
            calendar.setTime(calendarView.selectedDate.get());
        }
        int selectedDay = calendar.get(Calendar.DATE);
        int selectedMonth = calendar.get(Calendar.MONTH);
        int selectedYear = calendar.get(Calendar.YEAR);
        
        calendar.setTime(tmp);

        // Set the calendar to the end of the previous month.
        while (calendar.getFirstDayOfWeek() != calendar.get(Calendar.DAY_OF_WEEK)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Ignore the week day row and the week number column
        for (int i = numberOfDaysPerWeek + (calendarView.getShowWeeks() ? 1 : 0); i < getChildren().size(); i++) {
            if (i % (numberOfDaysPerWeek + 1) == 0 && (calendarView.getShowWeeks())) {
                Label label = (Label) getChildren().get(i);
                label.setText(Integer.toString(calendar.get(Calendar.WEEK_OF_YEAR)));
            } else {
                final GridPane gridBtn = (GridPane) getChildren().get(i);

                gridBtn.getChildren().clear();
                gridBtn.add(new Label(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))), 1, 0);

                boolean disabled = calendarView.getDisabledWeekdays().contains(calendar.get(Calendar.DAY_OF_WEEK));

                for (Date disabledDate : calendarView.getDisabledDates()) {
                    Calendar clone = (Calendar) calendar.clone();
                    clone.setTime(disabledDate);
                    if (calendar.get(Calendar.YEAR) == clone.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == clone.get(Calendar.MONTH) && calendar.get(Calendar.DAY_OF_MONTH) == clone.get(Calendar.DAY_OF_MONTH)) {
                        disabled = true;
                    }
                }
                gridBtn.setDisable(disabled);

                gridBtn.getStyleClass().remove(CSS_CALENDAR_DAY_CURRENT_MONTH);
                gridBtn.getStyleClass().remove(CSS_CALENDAR_DAY_OTHER_MONTH);
                gridBtn.getStyleClass().remove(CSS_CALENDAR_TODAY);
                gridBtn.getStyleClass().remove(CSS_CALENDAR_SELECTED);
                gridBtn.getStyleClass().remove(CSS_CALENDAR_DAY_HAS_EVENTS);

                if (calendar.get(Calendar.MONTH) == month) {
                    gridBtn.getStyleClass().add(CSS_CALENDAR_DAY_CURRENT_MONTH);
                } else {
                    gridBtn.getStyleClass().add(CSS_CALENDAR_DAY_OTHER_MONTH);
                }

                if (calendar.get(Calendar.YEAR) == todayYear && calendar.get(Calendar.MONTH) == todayMonth && calendar.get(Calendar.DATE) == todayDay) {
                    gridBtn.getStyleClass().add(CSS_CALENDAR_TODAY);
                }
                if (calendar.get(Calendar.YEAR) == selectedYear && calendar.get(Calendar.MONTH) == selectedMonth && calendar.get(Calendar.DATE) == selectedDay) {
                    gridBtn.getStyleClass().add(CSS_CALENDAR_SELECTED);
                }

                final ObservableList<CalendarEvent> eventsForDate = calendarView.getCalendarEventsForCalendarDate(calendar);
                eventsForDate.addListener((ListChangeListener<CalendarEvent>) c -> updateDays());
                if (!eventsForDate.isEmpty()) {
                    if (calendar.get(Calendar.MONTH) == month) {
                        gridBtn.getStyleClass().add(CSS_CALENDAR_DAY_HAS_EVENTS);
                    }
                    final ScrollPane scrollEventsPane = new ScrollPane();
                    scrollEventsPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    scrollEventsPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scrollEventsPane.setFitToWidth(true);
                    scrollEventsPane.setFitToHeight(true);
                    final VBox eventsBox = new VBox();
                    eventsBox.setBorder(Border.EMPTY);
                    eventsBox.getStyleClass().add(CSS_CALENDAR_DAY_HAS_EVENTS);
                    eventsBox.setFillWidth(true);

                    for (final CalendarEvent calendarEvent : eventsForDate) {
                        final Button control = new Button();
                        if (calendarEvent.getImage() != null) {
                            final ImageView imgView = new ImageView(calendarEvent.getImage());
                            imgView.setFitHeight(16);
                            control.setGraphic(imgView);
                        }
                        control.setTextOverrun(OverrunStyle.ELLIPSIS);
                        control.setText(calendarEvent.getTitle());
                        control.setTextAlignment(TextAlignment.LEFT);
                        control.setTooltip(new Tooltip(calendarEvent.getTitle()));
                        control.setOnMouseClicked(event -> calendarView.showCalendarPopupForEvent(calendarEvent, control));
                        control.getStyleClass().add(CSS_CALENDAR_DAY_HAS_EVENTS);
                        control.setBorder(Border.EMPTY);
                        eventsBox.getChildren().add(control);
                    }
                    scrollEventsPane.setContent(eventsBox);
                    scrollEventsPane.getStyleClass().add(CSS_CALENDAR_DAY_HAS_EVENTS);
                    scrollEventsPane.setBorder(Border.EMPTY);
                    gridBtn.add(scrollEventsPane, 0, 1, GridPane.REMAINING, 1);
                } else {
                    gridBtn.getStyleClass().remove(CSS_CALENDAR_DAY_HAS_EVENTS);
                }

                gridBtn.setUserData(calendar.getTime());
                calendar.add(Calendar.DATE, 1);
            }
        }

        // Restore original date
        calendar.setTime(calendarView.calendarDate.get());
    }

    /**
     * Updates the week names, when the locale changed.
     */
    private void updateWeekNames() {
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(calendarView.localeProperty().get());
        String[] weekDays = dateFormatSymbols.getShortWeekdays();

        // Start with 1 instead of 0, since the first element in the array is empty.
        for (int i = 1; i < weekDays.length; i++) {
            String shortWeekDay = weekDays[i];

            // Shift the index according to the first day of week.
            int j = i - calendarView.getCalendar().getFirstDayOfWeek();
            if (j < 0) {
                j += weekDays.length - 1;
            }

            Label label = (Label) getChildren().get(j + (calendarView.getShowWeeks() ? 1 : 0));

            label.setText(shortWeekDay);
        }
        title.set(getDateFormat("MMMM yyyy").format(calendarView.getCalendar().getTime()));
    }
}
