import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

/**
 * Student Record Management System - Java console version.
 * Demonstrates OOP, ArrayList, Stack, linear search and bubble sort.
 * Run: javac Student.java && java Student
 *
 * This Java program is separate from the static GitHub Pages website.
 */
public class Student {
    private final int rollNumber;
    private final String name;
    private final String course;
    private final double marks;

    public Student(int rollNumber, String name, String course, double marks) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.course = course;
        this.marks = marks;
    }

    public int getRollNumber() { return rollNumber; }
    public String getName() { return name; }
    public String getCourse() { return course; }
    public double getMarks() { return marks; }

    @Override
    public String toString() {
        return String.format("Roll: %-5d | Name: %-20s | Course: %-14s | Marks: %.2f",
                rollNumber, name, course, marks);
    }

    public static void main(String[] args) {
        ArrayList<Student> students = new ArrayList<>();
        Stack<Student> deleted = new Stack<>();
        Scanner in = new Scanner(System.in);
        students.add(new Student(101, "Aarav Menon", "B.Tech CSE", 88));
        students.add(new Student(102, "Ananya Joseph", "B.Tech CSE", 94));
        students.add(new Student(103, "Rahul Das", "B.Tech ECE", 76));

        boolean running = true;
        while (running) {
            System.out.println("\n=== STUDENT RECORD MANAGEMENT SYSTEM ===");
            System.out.println("1. Display all students");
            System.out.println("2. Add student");
            System.out.println("3. Search by roll number");
            System.out.println("4. Sort by marks (bubble sort)");
            System.out.println("5. Delete student");
            System.out.println("6. Restore last deleted student (stack)");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            int choice = readInt(in);
            switch (choice) {
                case 1 -> display(students);
                case 2 -> add(in, students);
                case 3 -> {
                    System.out.print("Roll number to search: ");
                    Student s = find(students, readInt(in));
                    System.out.println(s == null ? "Student not found." : "Record found: " + s);
                }
                case 4 -> { bubbleSort(students); System.out.println("Sorted by marks, highest first."); display(students); }
                case 5 -> {
                    System.out.print("Roll number to delete: ");
                    Student s = find(students, readInt(in));
                    if (s == null) System.out.println("Student not found.");
                    else { students.remove(s); deleted.push(s); System.out.println("Record deleted."); }
                }
                case 6 -> {
                    if (deleted.empty()) System.out.println("No deleted record to restore.");
                    else {
                        Student s = deleted.peek();
                        if (find(students, s.getRollNumber()) != null) System.out.println("That roll number already exists.");
                        else { students.add(deleted.pop()); System.out.println("Last deleted record restored."); }
                    }
                }
                case 0 -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
        in.close();
        System.out.println("Program closed.");
    }

    private static int readInt(Scanner in) {
        while (!in.hasNextInt()) { System.out.print("Enter a whole number: "); in.next(); }
        return in.nextInt();
    }

    private static void display(ArrayList<Student> students) {
        if (students.isEmpty()) { System.out.println("No student records."); return; }
        for (Student s : students) System.out.println(s);
    }

    private static void add(Scanner in, ArrayList<Student> students) {
        System.out.print("Roll number: ");
        int roll = readInt(in);
        if (find(students, roll) != null) { System.out.println("That roll number already exists."); return; }
        in.nextLine();
        System.out.print("Student name: "); String name = in.nextLine().trim();
        System.out.print("Course/department: "); String course = in.nextLine().trim();
        System.out.print("Marks (0-100): ");
        double marks;
        while (!in.hasNextDouble()) { System.out.print("Enter a valid number: "); in.next(); }
        marks = in.nextDouble();
        if (name.isEmpty() || course.isEmpty() || marks < 0 || marks > 100) {
            System.out.println("Invalid name, course or marks."); return;
        }
        students.add(new Student(roll, name, course, marks));
        System.out.println("Student added successfully.");
    }

    // Linear search: checks each student one by one.
    private static Student find(ArrayList<Student> students, int roll) {
        for (Student s : students) if (s.getRollNumber() == roll) return s;
        return null;
    }

    // Bubble sort in descending order of marks.
    private static void bubbleSort(ArrayList<Student> students) {
        for (int i = 0; i < students.size() - 1; i++) {
            for (int j = 0; j < students.size() - 1 - i; j++) {
                if (students.get(j).getMarks() < students.get(j + 1).getMarks()) {
                    Student temp = students.get(j);
                    students.set(j, students.get(j + 1));
                    students.set(j + 1, temp);
                }
            }
        }
    }
}
