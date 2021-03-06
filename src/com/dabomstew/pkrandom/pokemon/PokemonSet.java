package com.dabomstew.pkrandom.pokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.dabomstew.pkrandom.exceptions.RandomizationException;

public class PokemonSet {
    public PokemonSet() {
        pokes = new ArrayList<Pokemon>();
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();
    }

    public PokemonSet(List<Pokemon> pks) {
        pokes = new ArrayList<Pokemon>();
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();

        for (Pokemon pk : pks) {
            if (pk != null) {
                pokes.add(pk);

                ArrayList<Pokemon> set = pokesByType.get(pk.primaryType);
                if (set == null) {
                    set = new ArrayList<Pokemon>();
                    pokesByType.put(pk.primaryType, set);
                }
                set.add(pk);

                if (pk.secondaryType != null) {
                    set = pokesByType.get(pk.secondaryType);
                    if (set == null) {
                        set = new ArrayList<Pokemon>();
                        pokesByType.put(pk.secondaryType, set);
                    }
                    set.add(pk);
                }
            }
        }

        updateTypeCount();
    }

    public PokemonSet(PokemonSet ps) {
        pokes = new ArrayList<Pokemon>(ps.pokes);
        pokesByType = new TreeMap<Type, ArrayList<Pokemon>>();
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : ps.pokesByType.entrySet()) {
            pokesByType.put(entry.getKey(), new ArrayList<Pokemon>(entry.getValue()));
        }
        typeCount = ps.typeCount;
    }

    public int size() {
        return pokes.size();
    }

    public int sizeByType(Type type) {
        ArrayList<Pokemon> list = pokesByType.get(type);
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    // Filtering
    public PokemonSet filterLegendaries() {
        filter(p -> p.isLegendary());
        return this;
    }

    public PokemonSet filterList(List<Pokemon> list) {
        if (!list.isEmpty()) {
            Set<Pokemon> set = new TreeSet<Pokemon>(list);
            filter(p -> set.contains(p));
        }
        return this;
    }

    public PokemonSet filterSet(Set<Pokemon> set) {
        if (!set.isEmpty()) {
            filter(p -> set.contains(p));
        }
        return this;
    }

    public PokemonSet filterByMinimumLevel(int level) {
        filter(p -> p.minimumLevel() > level);
        return this;
    }

    public PokemonSet remove(Pokemon poke) {
        filter(p -> p.equals(poke));
        return this;
    }

    public PokemonSet filter(Predicate<Pokemon> pred) {
        pokes.removeIf(pred);
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            entry.getValue().removeIf(pred);
        }

        pokesByType.entrySet().removeIf(e -> e.getValue().isEmpty());

        updateTypeCount();
        return this;
    }

    // Generation
    public Pokemon randomPokemon(Random random) {
        return pokes.get(random.nextInt(pokes.size()));
    }

    public Pokemon randomPokemonOfType(Type type, Random random) {
        ArrayList<Pokemon> list = pokesByType.get(type);
        if (list == null || list.size() == 0) {
            throw new RandomizationException("No pokemon in set of type: " + type);
        }
        return list.get(random.nextInt(list.size()));
    }

    public Pokemon randomPokemonTypeWeighted(Random random) {
        int idx = random.nextInt(typeCount);
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            if (idx < entry.getValue().size()) {
                return entry.getValue().get(idx);
            }
            idx -= entry.getValue().size();
        }

        throw new IllegalStateException(String.format("randomPokemonTypeWeighted: %d/%d", idx, typeCount));
    }

    public Pokemon randomPokemonByPowerLevel(Pokemon current, boolean banSamePokemon, Random random) {
        // start with within 10% and add 20% either direction till we find
        // something
        int currentBST = current.bstForPowerLevels();
        int minTarget = currentBST - currentBST / 10;
        int maxTarget = currentBST + currentBST / 10;
        Set<Pokemon> canPick = new TreeSet<Pokemon>();
        for (int expandRounds = 0; canPick.isEmpty() || (canPick.size() < 3 && expandRounds < 3); expandRounds++) {
            for (Pokemon pk : pokes) {
                if (pk.bstForPowerLevels() >= minTarget && pk.bstForPowerLevels() <= maxTarget
                        && (!banSamePokemon || pk != current)) {
                    canPick.add(pk);
                }
            }
            minTarget -= currentBST / 20;
            maxTarget += currentBST / 20;
        }
        return new ArrayList<Pokemon>(canPick).get(random.nextInt(canPick.size()));
    }

    public Type randomType(Random random) {
        ArrayList<Type> list = new ArrayList<Type>(pokesByType.keySet());
        return list.get(random.nextInt(list.size()));
    }

    public Type randomType(int minSize, Random random) {
        ArrayList<Type> list = new ArrayList<Type>(pokesByType.keySet());
        list.removeIf(t -> pokesByType.get(t).size() < minSize);
        return list.get(random.nextInt(list.size()));
    }

    public Type randomTypeWeighted(Random random) {
        int idx = random.nextInt(typeCount);
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            if (idx < entry.getValue().size()) {
                return entry.getKey();
            }
            idx -= entry.getValue().size();
        }
        throw new IllegalStateException(String.format("randomTypeWeighted: %d/%d", idx, typeCount));
    }

    // Internal
    private void updateTypeCount() {
        typeCount = 0;
        for (Map.Entry<Type, ArrayList<Pokemon>> entry : pokesByType.entrySet()) {
            typeCount += entry.getValue().size();
        }
    }

    private ArrayList<Pokemon> pokes;
    private Map<Type, ArrayList<Pokemon>> pokesByType;
    private int typeCount;
}
