package middleware.server;

import java.util.Objects;

public class Pair<F,S> {
    private final F first;
    private final S second;

    /**
     * Parameterized constructor for Pair
     * */
    public Pair(F first, S second){
        this.first = first;
        this.second = second;
    }

    /**
     * Method that gets the first element of a pair
     *
     * @return F Object that represents the first element of the pair
     * */
    public F getFirst() {
        return this.first;
    }

    /**
     * Method that gets the second element of a pair
     *
     * @return F Object that represents the second element of the pair
     * */
    public S getSecond() {
        return this.second;
    }

    /**
     * Method that tests if a Pair is equal to another Pair
     *
     * @return boolean Boolean that represents the status of the operation.
     * */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return getFirst().equals(pair.getFirst()) && getSecond().equals(pair.getSecond());
    }

    /**
     * Method that calculates the hash of a Pair.
     *
     * @return int Integer that represents the hash of a Pair.
     * */
    @Override
    public int hashCode() {
        return Objects.hash(getFirst(), getSecond());
    }
}
