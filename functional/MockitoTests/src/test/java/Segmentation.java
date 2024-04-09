package test.java;

import java.util.ArrayList;
import org.mockito.Mockito;

public class Segmentation {
    public static void main(String[] args) {
        System.out.println("This is to trigger a SegmentationError!");
        Mockito.mock(ArrayList.class);
    }

}

