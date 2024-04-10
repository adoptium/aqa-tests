package test.java;

import java.util.ArrayList;
import org.mockito.Mockito;

public class MockitoMockTest {
    public static void main(String[] args) {
        System.out.println("Call Mockito.mock(ArrayList.class)");
        Mockito.mock(ArrayList.class);
    }
}