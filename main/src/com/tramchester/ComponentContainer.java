package com.tramchester;

public interface ComponentContainer {
    void initialise();
    <C> C get(Class<C> klass);
    void close();
    void registerCallbackFor(ClosesResource closesResource);

    interface ClosesResource {
        void close();
    }
}
