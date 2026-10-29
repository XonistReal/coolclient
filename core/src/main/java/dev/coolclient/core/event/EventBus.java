package dev.coolclient.core.event;

import dev.coolclient.core.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Small typed event bus. Listeners may be tied to an {@link Owner} (usually a module) and
 * only receive events while that owner is active.
 */
public final class EventBus {
    public interface Listener<E> {
        void handle(E event);
    }

    public interface Owner {
        boolean isListening();
    }

    private static final Comparator<Entry<?>> BY_PRIORITY = new Comparator<Entry<?>>() {
        @Override
        public int compare(Entry<?> a, Entry<?> b) {
            return Integer.compare(b.priority, a.priority);
        }
    };

    private static final class Entry<E> {
        final Owner owner;
        final Listener<E> listener;
        final int priority;

        Entry(Owner owner, Listener<E> listener, int priority) {
            this.owner = owner;
            this.listener = listener;
            this.priority = priority;
        }
    }

    private final Map<Class<?>, CopyOnWriteArrayList<Entry<?>>> listeners =
            new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Entry<?>>>();

    public <E> void on(Class<E> type, Listener<E> listener) {
        on(type, null, 0, listener);
    }

    public <E> void on(Class<E> type, Owner owner, int priority, Listener<E> listener) {
        CopyOnWriteArrayList<Entry<?>> list = listeners.get(type);
        if (list == null) {
            list = new CopyOnWriteArrayList<Entry<?>>();
            listeners.put(type, list);
        }
        List<Entry<?>> sorted = new ArrayList<Entry<?>>(list);
        sorted.add(new Entry<E>(owner, listener, priority));
        Collections.sort(sorted, BY_PRIORITY);
        list.clear();
        list.addAll(sorted);
    }

    @SuppressWarnings("unchecked")
    public <E> E post(E event) {
        List<Entry<?>> list = listeners.get(event.getClass());
        if (list == null) return event;
        for (Entry<?> raw : list) {
            Entry<E> e = (Entry<E>) raw;
            if (e.owner != null && !e.owner.isListening()) continue;
            try {
                e.listener.handle(event);
            } catch (Throwable t) {
                Log.error("Listener failed for " + event.getClass().getSimpleName(), t);
            }
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) break;
        }
        return event;
    }
}
