1
import com.schoolcanteen.dao.DatabaseInitializer;
import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.ui.Authentication;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // 1. Initialize the database first.
        DatabaseInitializer.initialize();

        // 2. Create shared objects.
        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);

        // 3. Start authentication.
        Authentication authentication =
                new Authentication(userDao, scanner);

        authentication.start();

        scanner.close();
    }
}
