package dev.voxelgame.common.gameplay.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public record StatusEffectState(List<ActiveStatusEffect> effects) {
    public StatusEffectState {
        effects = normalize(effects == null ? List.of() : effects);
    }

    public static StatusEffectState empty() {
        return new StatusEffectState(List.of());
    }

    public static StatusEffectState fromSaveStates(List<StatusEffectSaveState> saveStates) {
        if (saveStates == null || saveStates.isEmpty()) {
            return empty();
        }
        List<ActiveStatusEffect> restored = new ArrayList<>(saveStates.size());
        for (StatusEffectSaveState saveState : saveStates) {
            restored.add(saveState.toActiveEffect());
        }
        return new StatusEffectState(restored);
    }

    public Optional<ActiveStatusEffect> find(StatusEffectType type) {
        if (type == null) {
            return Optional.empty();
        }
        for (ActiveStatusEffect effect : effects) {
            if (effect.type() == type) {
                return Optional.of(effect);
            }
        }
        return Optional.empty();
    }

    public boolean has(StatusEffectType type) {
        return find(type).isPresent();
    }

    public StatusEffectState apply(StatusEffectType type) {
        return apply(StatusEffectDefinitions.require(type));
    }

    public StatusEffectState apply(StatusEffectType type, double durationSeconds, int intensity) {
        StatusEffectDefinition definition = StatusEffectDefinitions.require(type);
        return apply(definition, durationSeconds, intensity);
    }

    public StatusEffectState apply(StatusEffectDefinition definition) {
        return apply(definition, definition.defaultDurationSeconds(), 1);
    }

    public StatusEffectState apply(StatusEffectDefinition definition, double durationSeconds, int intensity) {
        ActiveStatusEffect incoming = definition.create(durationSeconds, intensity);
        EnumMap<StatusEffectType, ActiveStatusEffect> map = asMap();
        ActiveStatusEffect current = map.get(definition.type());
        map.put(definition.type(), current == null ? incoming : merge(definition, current, incoming));
        return fromMap(map);
    }

    public StatusEffectState remove(StatusEffectType type) {
        if (type == null || effects.isEmpty()) {
            return this;
        }
        EnumMap<StatusEffectType, ActiveStatusEffect> map = asMap();
        map.remove(type);
        return fromMap(map);
    }

    public StatusEffectTickResult tick(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }
        if (deltaSeconds == 0.0 || effects.isEmpty()) {
            return new StatusEffectTickResult(this, List.of());
        }
        List<ActiveStatusEffect> next = new ArrayList<>(effects.size());
        List<StatusEffectPulse> pulses = new ArrayList<>();
        for (ActiveStatusEffect active : effects) {
            StatusEffectDefinition definition = StatusEffectDefinitions.require(active.type());
            double elapsed = Math.min(deltaSeconds, active.remainingSeconds());
            double remaining = active.remainingSeconds() - deltaSeconds;
            double tickProgress = active.tickProgressSeconds() + elapsed;
            if (!definition.tickEffect().isEmpty()) {
                while (tickProgress + 0.0000001 >= definition.tickIntervalSeconds()) {
                    tickProgress -= definition.tickIntervalSeconds();
                    pulses.add(new StatusEffectPulse(
                            active.type(),
                            active.intensity(),
                            definition.tickEffect().scaledByIntensity(active.intensity())
                    ));
                }
            }
            if (remaining > 0.0000001) {
                next.add(new ActiveStatusEffect(active.type(), remaining, active.intensity(), tickProgress));
            }
        }
        return new StatusEffectTickResult(new StatusEffectState(next), pulses);
    }

    public StatusEffectModifiers combinedModifiers() {
        StatusEffectModifiers modifiers = StatusEffectModifiers.NEUTRAL;
        for (ActiveStatusEffect active : effects) {
            StatusEffectDefinition definition = StatusEffectDefinitions.require(active.type());
            modifiers = modifiers.multiply(definition.modifiers().scaledByIntensity(active.intensity()));
        }
        return modifiers;
    }

    public List<StatusEffectSaveState> saveStates() {
        return effects.stream()
                .map(ActiveStatusEffect::toSaveState)
                .toList();
    }

    private static ActiveStatusEffect merge(
            StatusEffectDefinition definition,
            ActiveStatusEffect current,
            ActiveStatusEffect incoming
    ) {
        int intensity = Math.min(definition.maxIntensity(), Math.max(current.intensity(), incoming.intensity()));
        double remaining = switch (definition.stackRule()) {
            case REFRESH_DURATION -> Math.max(current.remainingSeconds(), incoming.remainingSeconds());
            case EXTEND_DURATION -> Math.min(definition.maxDurationSeconds(), current.remainingSeconds() + incoming.remainingSeconds());
            case INCREASE_INTENSITY_REFRESH_DURATION -> {
                intensity = Math.min(definition.maxIntensity(), current.intensity() + incoming.intensity());
                yield Math.max(current.remainingSeconds(), incoming.remainingSeconds());
            }
        };
        return new ActiveStatusEffect(definition.type(), remaining, intensity, current.tickProgressSeconds());
    }

    private EnumMap<StatusEffectType, ActiveStatusEffect> asMap() {
        EnumMap<StatusEffectType, ActiveStatusEffect> map = new EnumMap<>(StatusEffectType.class);
        for (ActiveStatusEffect effect : effects) {
            map.put(effect.type(), effect);
        }
        return map;
    }

    private static StatusEffectState fromMap(EnumMap<StatusEffectType, ActiveStatusEffect> map) {
        return new StatusEffectState(new ArrayList<>(map.values()));
    }

    private static List<ActiveStatusEffect> normalize(List<ActiveStatusEffect> source) {
        EnumMap<StatusEffectType, ActiveStatusEffect> byType = new EnumMap<>(StatusEffectType.class);
        for (ActiveStatusEffect effect : source) {
            if (effect == null) {
                throw new NullPointerException("effects must not contain null");
            }
            StatusEffectDefinitions.require(effect.type());
            ActiveStatusEffect previous = byType.put(effect.type(), effect);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate status effect: " + effect.type());
            }
        }
        List<ActiveStatusEffect> normalized = new ArrayList<>(byType.values());
        normalized.sort(Comparator.comparingInt(effect -> effect.type().ordinal()));
        return List.copyOf(normalized);
    }
}
