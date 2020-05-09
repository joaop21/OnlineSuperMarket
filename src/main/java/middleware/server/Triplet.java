package middleware.server;

public class Triplet<F,S,T> extends Pair<F,S>{
    private final T third;

    /**
     * Parameterized constructor for Pair
     * */
    public Triplet(F first, S second, T third){
        super(first, second);
        this.third = third;
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
