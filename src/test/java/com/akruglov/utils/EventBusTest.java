package com.akruglov.utils;

import com.akruglov.utils.exceptions.EventBusSubscriptionNotFoundException;
import com.google.common.collect.HashMultimap;

import java.util.Set;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest(EventBusTest.FakeListener.class)
public class EventBusTest {
        
    private EventBus bus;
    private FakeListener fakeListener;
    
    @Mock
    private FakeListener mockListener;
    
    public static class FakeListener {

        public void use(String s) {

        }

        public void put(String s) {

        }

        public static void save(String s) {

        }
    }
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        bus = new EventBus();
        fakeListener = new FakeListener();
    }
    
    @Test
    public void subscribeShouldAddFreeConsumer() {
        Consumer<String> consumer = (s) -> System.out.println(s);
        bus.subscribe(String.class, consumer);
        
        HashMultimap<Class, Consumer> freeConsumers =
                Whitebox.getInternalState(bus, "freeConsumers");
        
        Set<Consumer> consumersForEvent = freeConsumers.get(String.class);
        Consumer<String> addedConsumer = consumersForEvent.iterator().next();
        
        assertEquals(freeConsumers.size(), 1);
        assertEquals(consumersForEvent.size(), 1);
        assertSame(addedConsumer, consumer);
    }
    
    @Test
    public void subscribeShouldAddObjectAndConsumer() {
        Consumer<String> consumer = fakeListener::use;

        bus.subscribe(String.class, fakeListener, consumer);

        HashMultimap<Class, Object> registrations = Whitebox.getInternalState(bus, "registrations");
        
        Set<Object> listenersForEvent = registrations.get(String.class);
        Object listener  = listenersForEvent.iterator().next();
        
        HashMultimap<Object, Consumer> consumers = Whitebox.getInternalState(bus, "consumers");
        
        Set<Consumer> consumersForListener = consumers.get(listener);
        Consumer<String> addedConsumer = consumersForListener.iterator().next();
        
        assertEquals(registrations.size(), 1);
        assertEquals(listenersForEvent.size(), 1);
        assertSame(listener, fakeListener);
        assertEquals(consumers.size(), 1);
        assertEquals(consumersForListener.size(), 1);
        assertSame(addedConsumer, consumer);
    }
    
    @Test
    public void unsubscribeShouldRemoveFreeConsumer() {
        Consumer<String> consumer = (s) -> System.out.println(s);
        bus.subscribe(String.class, consumer);
        bus.unsubscribe(String.class, consumer);
        
        HashMultimap<Class, Consumer> freeConsumers =
                Whitebox.getInternalState(bus, "freeConsumers");
        
        assertTrue(freeConsumers.isEmpty());
    }
    
    @Test(expected = EventBusSubscriptionNotFoundException.class)
    public void unsubscribeShouldThrowEventBusSubscriptionNotFoundExceptionForFreeConsumer() {
        Consumer<String> consumer = (s) -> System.out.println(s);
        bus.subscribe(String.class, (s) -> System.out.println(s));
        bus.unsubscribe(String.class, consumer);
    }
    
    @Test
    public void unsubscribeShouldRemoveOnlyOneConsumer() {
        Consumer<String> consumer1 = (s) -> System.out.println(s);
        Consumer<String> consumer2 = (s) -> System.out.println(s + " " +  s);
        bus.subscribe(String.class, consumer1);
        bus.subscribe(String.class, consumer2);
        bus.unsubscribe(String.class, consumer1);
        
        HashMultimap<Class, Consumer> freeConsumers =
                Whitebox.getInternalState(bus, "freeConsumers");
        
        Set<Consumer> consumersForEvent = freeConsumers.get(String.class);
        Consumer subscribedConsumer = consumersForEvent.iterator().next();
        
        assertEquals(freeConsumers.size(), 1);
        assertEquals(consumersForEvent.size(), 1);
        assertSame(subscribedConsumer, consumer2);
    }
    
    @Test
    public void unsubscribeShouldRemoveListenerAndConsumers() {
        bus.subscribe(String.class, fakeListener, fakeListener::use);
        bus.subscribe(String.class, fakeListener, fakeListener::put);
        bus.unsubscribe(String.class, fakeListener);

        HashMultimap<Class, Object> registrations = Whitebox.getInternalState(bus, "registrations");
        HashMultimap<Object, Consumer> consumers = Whitebox.getInternalState(bus, "consumers");

        assertTrue(registrations.isEmpty());
        assertTrue(consumers.isEmpty());
    }
    
    @Test(expected = EventBusSubscriptionNotFoundException.class)
    public void unsubscribeShouldThrowEventBusSubscriptionNotFoundExceptionForListener() {
        bus.subscribe(String.class, fakeListener, fakeListener::use);
        bus.unsubscribe(String.class, new FakeListener());
    }
    
    @Test
    public void unsubscribeShouldRemoveOnlyOneListenerWithItsCunsumers() {
        FakeListener anotherListener = new FakeListener();
        Consumer<String> firstConsumer = anotherListener::use;
        Consumer<String> secondConsumer = anotherListener::put;
        bus.subscribe(String.class, fakeListener, fakeListener::use);
        bus.subscribe(String.class, fakeListener, fakeListener::put);
        bus.subscribe(String.class, anotherListener, firstConsumer);
        bus.subscribe(String.class, anotherListener, secondConsumer);
        bus.unsubscribe(String.class, fakeListener);

        HashMultimap<Class, Object> registrations = Whitebox.getInternalState(bus, "registrations");
        HashMultimap<Object, Consumer> consumers = Whitebox.getInternalState(bus, "consumers");

        Set<Object> listenersForEvent = registrations.get(String.class);
        Object subscribedListener = listenersForEvent.iterator().next();

        Set<Consumer> consumersForListener = consumers.get(subscribedListener);

        assertEquals(registrations.size(), 1);
        assertEquals(listenersForEvent.size(), 1);
        assertSame(subscribedListener, anotherListener);
        assertEquals(consumers.size(), 2);
        assertEquals(consumersForListener.size(), 2);
        assertTrue(consumersForListener.contains(firstConsumer));
        assertTrue(consumersForListener.contains(secondConsumer));
    }
    
    @Test
    public void fireShouldCallAllConsumers() throws Exception {
        mockStatic(FakeListener.class);
        PowerMockito.doNothing().when(FakeListener.class, "save", Mockito.anyString());
        bus.subscribe(String.class, mockListener, mockListener::use);
        bus.subscribe(String.class, FakeListener::save);
        bus.fire("Test");

        verify(mockListener, times(1)).use(anyString());

        PowerMockito.verifyStatic();
        FakeListener.save(Mockito.anyString());
    }
}
