package test.java;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MainTest {

    class Some {}

    @Test
    public void reproduceCrash() {
        Mockito.mock(Some.class);
    }

}