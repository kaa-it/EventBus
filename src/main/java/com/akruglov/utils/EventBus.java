package com.akruglov.utils;

import com.akruglov.utils.exceptions.EventBusSubscriptionNotFoundException;
import com.google.common.collect.HashMultimap;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Implementation of event bus.
 *
 * Event bus can send events to all its subscribers.
 * Subscribers can be various objects or static methods, or lambdas.
 *
 * @author Andrey Kruglov
 */
public class EventBus {
    
    // For static methods and lambdas
    HashMultimap<Class, Consumer> freeConsumers;
    
    // For objects
    HashMultimap<Class, Object> registrations;
    HashMultimap<Object, Consumer> consumers;
    
    public EventBus() {
        freeConsumers = HashMultimap.create();
        registrations = HashMultimap.create();
        consumers = HashMultimap.create();
    }

    /**
     * Susbscribes an object to an event.
     *
     * @param eventClass class of event to subscribe
     * @param listener object to receive events of given class
     * @param consumer reference to method of the object that will handle events of given class
     */
    public <Event, Listener> void subscribe(Class<Event> eventClass,
            Listener listener, Consumer<Event> consumer) {
        
        Set<Object> listenersForEvent = registrations.get(eventClass);
        
        listenersForEvent.add(listener);
        
        Set<Consumer> consumersForListener = consumers.get(listener);
        
        consumersForListener.add(consumer);
    }

    /**
     * Subsribes static method or lamda to an event.
     *
     * NOTE: References to static methods or/and lambdas must be cached
     * by caller to unsubscribe in the future.
     *
     * @param eventClass class of event to subscribe
     * @param freeConsumer reference to static method or lambda that will handle events of given class
     */
    public <Event> void subscribe(Class<Event> eventClass,
            Consumer<Event> freeConsumer) {
        
        Set<Consumer> consumersForEvent = freeConsumers.get(eventClass);
        
        consumersForEvent.add(freeConsumer);
    }

    /**
     * Unsusbcribes an object from an event.
     *
     * Removes all handlers of the object that handle the event of given class.
     *
     * @param eventClass class of event to unsubscribe
     * @param listener object to unsubscribe from events of given class
     * @throws EventBusSubscriptionNotFoundException if given object is not subscribed to events of given class
     */
    public <Event, Listener> void unsubscribe(Class<Event> eventClass,
            Listener listener) throws EventBusSubscriptionNotFoundException {
        
        Set<Object> listenersForEvent = registrations.get(eventClass);
                
        if (!listenersForEvent.remove(listener)) {
            throw new EventBusSubscriptionNotFoundException();
        }
        
        consumers.removeAll(listener);
        
        if (listenersForEvent.isEmpty()) {
            registrations.removeAll(eventClass);
        }
    }

    /**
     * Unsubsribes a static method or lambda from an event.
     *
     * @param eventClass class of event to unsubscribe
     * @param freeConsumer reference to static method or lambda to unsubscribe from events of given class
     * @throws EventBusSubscriptionNotFoundException if given static method or lambda is not subscribed to
     *     event of given class
     */
    public <Event> void unsubscribe(Class<Event> eventClass,
            Consumer<Event> freeConsumer) throws EventBusSubscriptionNotFoundException {
        
        Set<Consumer> consumersForEvent = freeConsumers.get(eventClass);
        
        if (!consumersForEvent.remove(freeConsumer)) {
            throw new EventBusSubscriptionNotFoundException();
        }
        
        if (consumersForEvent.isEmpty()) {
            freeConsumers.removeAll(eventClass);
        }
    }

    /**
     * Notify all subscribed objects, static methods and lambdas about given event
     *
     * @param event event to notify
     */
    public <Event> void fire(Event event) {
        notifyFreeConsumers(event);        
        notifyRegisteredObjects(event);
    }

    private <Event> void notifyRegisteredObjects(Event event) {
        Set<Object> listenersForEvent = registrations.get(event.getClass());
        
        for (Object listener : listenersForEvent) {
            Set<Consumer> consumersForListener = consumers.get(listener);
            
            for (Consumer consumer : consumersForListener) {
                consumer.accept(event);
            }
        }
    }

    private <Event> void notifyFreeConsumers(Event event) {
        Set<Consumer> consumersForEvent = freeConsumers.get(event.getClass());
        
        for (Consumer consumer : consumersForEvent) {
            consumer.accept(event);
        }
    }
}
