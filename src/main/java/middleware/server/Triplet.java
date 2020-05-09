package middleware.server;

public class Triplet<F,S,T> {
    private final F first;
    private final S second;
    private final T third;

    /**
     * Parameterized constructor for Pair
     * */
    public Triplet(F first, S second, T third){
        this.first = first;
        this.second = second;
        this.third = third;
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
     * Method that gets the third element of a pair
     *
     * @return F Object that represents the third element of the pair
     * */
    public T getThird() {
        return third;
    }
}
