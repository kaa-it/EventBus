package com.akruglov.utils;

import com.akruglov.utils.exceptions.EventBusSubscriptionNotFoundException;
import com.google.common.collect.HashMultimap;
import java.util.Set;
import java.util.function.Consumer;


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
    
    public <Event, Listener> void subscribe(Class<Event> eventClass,
            Listener listener, Consumer<Event> consumer) {
        
        Set<Object> listenersForEvent = registrations.get(eventClass);
        
        listenersForEvent.add(listener);
        
        Set<Consumer> consumersForListener = consumers.get(listener);
        
        consumersForListener.add(consumer);
    }
    
    public <Event> void subscribe(Class<Event> eventClass,
            Consumer<Event> freeConsumer) {
        
        Set<Consumer> consumersForEvent = freeConsumers.get(eventClass);
        
        consumersForEvent.add(freeConsumer);
    }
    
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
