package middleware.server;

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
}
