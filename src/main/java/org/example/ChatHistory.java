package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatHistory {

    private final List<Event> events = Collections.synchronizedList(new ArrayList<>());

    public void add(Event event) {
        events.add(event);
        System.out.println(event.format());
    }

    public List<Event> getHistory() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    public void setHistory(List<Event> history) {
        for(Event event : history) {
            add(event);
        }
    }
}