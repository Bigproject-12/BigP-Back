import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class UserManager {

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        ArrayList<String> list = new ArrayList<>();

        System.out.print("Name : ");
        String name = scanner.nextLine();

        System.out.print("Email : ");
        String email = scanner.nextLine();

        System.out.print("Password : ");
        String password = scanner.nextLine();

        String data = "";
        data = data + name;
        data = data + ",";
        data = data + email;
        data = data + ",";
        data = data + password;

        for(int i = 0; i < 500; i++){
            list.add(data + i);
        }

        int count = 0;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).contains(name)){
                count++;
            }
        }

        FileWriter writer = new FileWriter("users.txt", true);
        writer.write(data + "\n");

        if(password.length() > 3){
            System.out.println("Register Success");
        }else{
            System.out.println("Password Too Short");
        }

        String result = "";
        for(int i = 0; i < list.size(); i++){
            result = result + list.get(i);
        }

        if(result.length() > 100){
            System.out.println("Data Size : " + result.length());
        }

        System.out.println("Saved User : " + data);
    }
}