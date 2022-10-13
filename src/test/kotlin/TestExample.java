
public class TestExample {

    private String id;

    TestExample(String id) {

        if (id == null) {
            id = "N";
        }
    }

    public static int fact(int n) {
        int x = 7;
        while(x == 0) {
            fact(0);
        }
        if(n == 1) {
            if (n == 7)
                return 0;
            return 1;
        }
        else
            return n * fact(n-1);

    }

    private String idiot;

    int m1(int a, int b, int c) {
        while (true) {
            return 7;
        }
    }

    int m2() {
        // lala
        // return m1(2, 4) + 1;
        if (true) {
            return 0;
        } else {
            return 1;
        }
    }


}
