package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatHistory {

    private static final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    public static void add(Event event) {
        events.add(event);
        System.out.println(event.format());
    }

    public static List<Event> getEvents() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    public static void addEventsFromHistory(List<Event> historyEvents) {
        events.addAll(historyEvents);
        events.sort(Comparator.comparing(Event::time));
    }
}