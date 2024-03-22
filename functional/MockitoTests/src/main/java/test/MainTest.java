package main.java.test;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
public class MainTest {
    class Some {}

    @Test
    public void reproduceCrash() {
        Mockito.mock(Some.class);
    }
}