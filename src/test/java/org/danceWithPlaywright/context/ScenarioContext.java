package org.danceWithPlaywright.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Getter
@Setter
public class ScenarioContext {

    private final HashMap<ContextKey, Object> context = new HashMap<>();

    public void save(ContextKey key, Object value) {
        context.put(key, value);
    }

    public <T> T get(ContextKey key, Class<T> type) {
        Object value = context.get(key);

        if (value == null) throw new IllegalArgumentException("No value stored for key: " + key);


        if (!type.isInstance(value)) {
            throw new IllegalStateException(
                    "Expected " + type.getSimpleName() +
                            " for key " + key + ", but got: " + value.getClass().getSimpleName());
        }

        return type.cast(value);
    }
}
