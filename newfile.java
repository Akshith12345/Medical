import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Scanner;

public class MedicineAppointmentManager {

    private static final String DATA_FILE = "health_manager.dat";
    private static ArrayList<ReminderItem> records = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        loadData();
        startReminderChecker();

        System.out.println("========================================");
        System.out.println("   HEALTH & APPOINTMENT MANAGER");
        System.out.println("========================================");

        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. View All Medicines (Compliance & Stock)");
            System.out.println("2. View All Appointments & Doctor Advice");
            System.out.println("3. View All Medical Records & Documents");
            System.out.println("4. View Emergency Contacts");
            System.out.println("5. View Daily Vitals Logs");
            System.out.println("6. Mark Today's Medicine as Taken");
            System.out.println("7. Add Medicine (with Time & Stock)");
            System.out.println("8. Add Appointment (with Symptoms & Notes)");
            System.out.println("9. Add Medical Record (with File Path)");
            System.out.println("10. Add Emergency Contact");
            System.out.println("11. Add Daily Vitals Entry");
            System.out.println("12. Export Full Health Report (.txt)");
            System.out.println("13. Backup Data File");
            System.out.println("14. Restore Data File");
            System.out.println("15. Delete a Record");
            System.out.println("16. View Upcoming Appointments");
            System.out.println("17. Exit & Save");
            System.out.print("Choose an option (1-17): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": viewMedicines(); break;
                case "2": viewAppointments(); break;
                case "3": viewMedicalRecords(); break;
                case "4": viewContacts(); break;
                case "5": viewVitals(); break;
                case "6": markMedicineTaken(); break;
                case "7": addMedicine(); break;
                case "8": addAppointment(); break;
                case "9": addMedicalRecord(); break;
                case "10": addContact(); break;
                case "11": addVitals(); break;
                case "12": exportReport(); break;
                case "13": backupData(); break;
                case "14": restoreData(); break;
                case "15": deleteRecord(); break;
                case "16": showReminders(); break;
                case "17":
                    running = false;
                    saveData();
                    System.out.println("Data saved. Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 17.");
            }
        }
    }

    private static void startReminderChecker() {
        Thread checkerThread = new Thread(() -> {
            while (running) {
                try {
                    LocalTime now = LocalTime.now().withSecond(0).withNano(0);
                    
                    synchronized (records) {
                        for (ReminderItem item : records) {
                            if (item instanceof Medicine) {
                                Medicine med = (Medicine) item;
                                if (med.getTime() != null && med.getTime().equals(now)) {
                                    System.out.println("\n\u001B[31m*** REMINDER: Time to take medicine -> " 
                                        + med.getName() + " (" + med.getDosage() + ") ***\u001B[0m\n");
                                    
                                    med.decrementStock();
                                    med.setTakenToday(false); // Reset taken flag for the scheduled dose
                                    saveData();
                                    
                                    if (med.getStock() <= 5 && med.getStock() >= 0) {
                                        System.out.println("\u001B[33m*** LOW STOCK WARNING: " + med.getName() + " only has " + med.getStock() + " pills left! ***\u001B[0m\n");
                                    }

                                    Thread.sleep(60000); 
                                }
                            }
                        }
                    }
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        });
        checkerThread.setDaemon(true);
        checkerThread.start();
    }

    private static void viewMedicines() {
        System.out.println("\n--- MEDICINES LIST ---");
        int count = 0;
        for (int i = 0; i < records.size(); i++) {
            ReminderItem item = records.get(i);
            if (item instanceof Medicine) {
                System.out.println("[" + i + "] " + item.getSummary());
                count++;
            }
        }
        if (count == 0) System.out.println("No medicines recorded.");
    }

    private static void viewAppointments() {
        System.out.println("\n--- APPOINTMENTS LIST ---");
        int count = 0;
        for (int i = 0; i < records.size(); i++) {
            ReminderItem item = records.get(i);
            if (item instanceof Appointment) {
                System.out.println("[" + i + "] " + item.getSummary());
                count++;
            }
        }
        if (count == 0) System.out.println("No appointments recorded.");
    }

    private static void viewMedicalRecords() {
        System.out.println("\n--- MEDICAL RECORDS & DOCUMENTS ---");
        int count = 0;
        for (int i = 0; i < records.size(); i++) {
            ReminderItem item = records.get(i);
            if (item instanceof MedicalRecord) {
                System.out.println("[" + i + "] " + item.getSummary());
                count++;
            }
        }
        if (count == 0) System.out.println("No medical records saved.");
    }

    private static void viewContacts() {
        System.out.println("\n--- EMERGENCY CONTACTS ---");
        int count = 0;
        for (int i = 0; i < records.size(); i++) {
            ReminderItem item = records.get(i);
            if (item instanceof EmergencyContact) {
                System.out.println("[" + i + "] " + item.getSummary());
                count++;
            }
        }
        if (count == 0) System.out.println("No emergency contacts saved.");
    }

    private static void viewVitals() {
        System.out.println("\n--- DAILY HEALTH VITALS LOGS ---");
        int count = 0;
        for (int i = 0; i < records.size(); i++) {
            ReminderItem item = records.get(i);
            if (item instanceof HealthVitals) {
                System.out.println("[" + i + "] " + item.getSummary());
                count++;
            }
        }
        if (count == 0) System.out.println("No vitals logged yet.");
    }

    private static void markMedicineTaken() {
        viewMedicines();
        System.out.print("Enter index of medicine to mark as TAKEN today: ");
        try {
            int index = Integer.parseInt(scanner.nextLine().trim());
            synchronized (records) {
                int medCount = 0;
                for (ReminderItem item : records) {
                    if (item instanceof Medicine) {
                        if (medCount == index) {
                            ((Medicine) item).setTakenToday(true);
                            saveData();
                            System.out.println("Medicine marked as taken for today!");
                            return;
                        }
                        medCount++;
                    }
                }
            }
            System.out.println("Invalid index.");
        } catch (Exception e) {
            System.out.println("Invalid input.");
        }
    }

    private static void addMedicine() {
        System.out.print("Enter medicine name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) { System.out.println("Error: Name cannot be empty."); return; }

        System.out.print("Enter dosage (e.g., 1 tablet): ");
        String dosage = scanner.nextLine().trim();

        try {
            System.out.print("Enter reminder time (HH:MM, e.g., 08:30): ");
            LocalTime time = LocalTime.parse(scanner.nextLine().trim());

            System.out.print("Enter initial pill quantity/stock (e.g., 30): ");
            int stock = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter instructions (e.g., After lunch): ");
            String schedule = scanner.nextLine().trim();

            synchronized (records) {
                records.add(new Medicine(name, dosage, time, stock, schedule));
            }
            saveData();
            System.out.println("Medicine added successfully!");
        } catch (Exception ex) {
            System.out.println("Invalid input format.");
        }
    }

    private static void addAppointment() {
        System.out.print("Enter doctor name: ");
        String doctor = scanner.nextLine().trim();
        if (doctor.isEmpty()) { return; }

        try {
            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(scanner.nextLine().trim());
            System.out.print("Enter time (HH:MM): ");
            LocalTime time = LocalTime.parse(scanner.nextLine().trim());
            System.out.print("Enter symptoms to discuss: ");
            String symptoms = scanner.nextLine().trim();
            System.out.print("Enter doctor's post-visit advice/notes: ");
            String advice = scanner.nextLine().trim();

            synchronized (records) {
                records.add(new Appointment(doctor, date, time, symptoms, advice));
            }
            saveData();
            System.out.println("Appointment added successfully!");
        } catch (Exception ex) {
            System.out.println("Invalid date or time format.");
        }
    }

    private static void addMedicalRecord() {
        System.out.print("Enter record title (e.g., Blood Test Report): ");
        String title = scanner.nextLine().trim();
        try {
            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(scanner.nextLine().trim());
            System.out.print("Enter provider/hospital: ");
            String provider = scanner.nextLine().trim();
            System.out.print("Enter notes/results summary: ");
            String notes = scanner.nextLine().trim();
            System.out.print("Enter file path/name of prescription or scan image: ");
            String filePath = scanner.nextLine().trim();

            synchronized (records) {
                records.add(new MedicalRecord(title, date, provider, notes, filePath));
            }
            saveData();
            System.out.println("Medical record and document path added successfully!");
        } catch (Exception ex) {
            System.out.println("Invalid date format.");
        }
    }

    private static void addContact() {
        System.out.print("Enter contact name (e.g., Dr. Smith / City Pharmacy): ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter relationship/role: ");
        String role = scanner.nextLine().trim();
        System.out.print("Enter phone number: ");
        String phone = scanner.nextLine().trim();

        synchronized (records) {
            records.add(new EmergencyContact(name, role, phone));
        }
        saveData();
        System.out.println("Emergency contact saved successfully!");
    }

    private static void addVitals() {
        try {
            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = LocalDate.parse(scanner.nextLine().trim());
            System.out.print("Enter Blood Pressure (e.g., 120/80): ");
            String bp = scanner.nextLine().trim();
            System.out.print("Enter Blood Sugar (e.g., 95 mg/dL): ");
            String sugar = scanner.nextLine().trim();
            System.out.print("Enter Temperature (e.g., 98.6 F): ");
            String temp = scanner.nextLine().trim();

            synchronized (records) {
                records.add(new HealthVitals(date, bp, sugar, temp));
            }
            saveData();
            System.out.println("Vitals logged successfully!");
        } catch (Exception ex) {
            System.out.println("Invalid date format.");
        }
    }

    private static void exportReport() {
        String filename = "Health_Report.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=========================================");
            writer.println("       PATIENT HEALTH SUMMARY REPORT     ");
            writer.println("=========================================\n");

            synchronized (records) {
                for (ReminderItem item : records) {
                    writer.println(item.getSummary());
                }
            }
            System.out.println("Report successfully exported to " + filename + "!");
        } catch (IOException e) {
            System.out.println("Could not export report: " + e.getMessage());
        }
    }

    private static void backupData() {
        System.out.print("Enter backup file name (e.g., health_backup.dat): ");
        String backupName = scanner.nextLine().trim();
        if (backupName.isEmpty()) backupName = "health_backup.dat";

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(backupName))) {
            synchronized (records) {
                out.writeObject(records);
            }
            System.out.println("Backup created successfully as " + backupName);
        } catch (IOException e) {
            System.out.println("Backup failed: " + e.getMessage());
        }
    }

    private static void restoreData() {
        System.out.print("Enter backup file name to restore from: ");
        String backupName = scanner.nextLine().trim();
        File file = new File(backupName);

        if (!file.exists()) {
            System.out.println("Backup file not found.");
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            synchronized (records) {
                records = (ArrayList<ReminderItem>) in.readObject();
            }
            saveData();
            System.out.println("Data restored successfully from " + backupName);
        } catch (Exception e) {
            System.out.println("Restore failed: " + e.getMessage());
        }
    }

    private static void deleteRecord() {
        System.out.println("\n--- ALL RECORDS ---");
        if (records.isEmpty()) {
            System.out.println("No records available.");
            return;
        }

        synchronized (records) {
            for (int i = 0; i < records.size(); i++) {
                System.out.println("[" + i + "] " + records.get(i).getSummary());
            }

            System.out.print("Enter index to delete: ");
            try {
                int index = Integer.parseInt(scanner.nextLine().trim());
                if (index >= 0 && index < records.size()) {
                    records.remove(index);
                    saveData();
                    System.out.println("Record deleted!");
                } else {
                    System.out.println("Invalid index.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private static void showReminders() {
        System.out.println("\n--- UPCOMING APPOINTMENTS ---");
        LocalDate today = LocalDate.now();
        boolean found = false;

        synchronized (records) {
            for (ReminderItem item : records) {
                if (item instanceof Appointment) {
                    Appointment a = (Appointment) item;
                    if (!a.getDate().isBefore(today)) {
                        System.out.println("-> " + a.getSummary());
                        found = true;
                    }
                }
            }
        }
        if (!found) System.out.println("No upcoming appointments.");
    }

    private static void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            synchronized (records) { out.writeObject(records); }
        } catch (IOException ex) {
            System.out.println("Save error: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            records = (ArrayList<ReminderItem>) in.readObject();
        } catch (Exception ex) {
            records = new ArrayList<>();
            file.delete();
        }
    }

    public abstract static class ReminderItem implements Serializable {
        private static final long serialVersionUID = 1L;
        public abstract String getSummary();
    }

    public static class Medicine extends ReminderItem {
        private static final long serialVersionUID = 4L;
        private String name, dosage, schedule;
        private LocalTime time;
        private int stock;
        private boolean takenToday;

        public Medicine(String name, String dosage, LocalTime time, int stock, String schedule) {
            this.name = name; this.dosage = dosage; this.time = time; this.stock = stock; this.schedule = schedule;
            this.takenToday = false;
        }

        public String getName() { return name; }
        public String getDosage() { return dosage; }
        public LocalTime getTime() { return time; }
        public int getStock() { return stock; }
        public void decrementStock() { if (stock > 0) stock--; }
        public void setTakenToday(boolean takenToday) { this.takenToday = takenToday; }

        @Override
        public String getSummary() {
            String status = takenToday ? "[TAKEN]" : "[PENDING]";
            return "[Medicine] " + status + " Name: " + name + " | Dosage: " + dosage + " | Time: " + time + " | Stock: " + stock + " pills | Notes: " + schedule;
        }
    }

    public static class Appointment extends ReminderItem {
        private static final long serialVersionUID = 4L;
        private String doctor, symptoms, advice;
        private LocalDate date;
        private LocalTime time;

        public Appointment(String doctor, LocalDate date, LocalTime time, String symptoms, String advice) {
            this.doctor = doctor; this.date = date; this.time = time; this.symptoms = symptoms; this.advice = advice;
        }

        public LocalDate getDate() { return date; }

        @Override
        public String getSummary() {
            return "[Appointment] Doctor: " + doctor + " | Date: " + date + " | Time: " + time + " | Symptoms: " + symptoms + " | Advice/Notes: " + advice;
        }
    }

    public static class MedicalRecord extends ReminderItem {
        private static final long serialVersionUID = 3L;
        private String title, provider, notes, filePath;
        private LocalDate date;

        public MedicalRecord(String title, LocalDate date, String provider, String notes, String filePath) {
            this.title = title; this.date = date; this.provider = provider; this.notes = notes; this.filePath = filePath;
        }

        @Override
        public String getSummary() {
            return "[Medical Record] " + title + " | Date: " + date + " | Provider: " + provider + " | Notes: " + notes + " | File/Scan: " + filePath;
        }
    }

    public static class EmergencyContact extends ReminderItem {
        private static final long serialVersionUID = 1L;
        private String name, role, phone;

        public EmergencyContact(String name, String role, String phone) {
            this.name = name; this.role = role; this.phone = phone;
        }

        @Override
        public String getSummary() {
            return "[Contact] " + name + " (" + role + ") - Phone: " + phone;
        }
    }

    public static class HealthVitals extends ReminderItem {
        private static final long serialVersionUID = 1L;
        private LocalDate date;
        private String bp, sugar, temp;

        public HealthVitals(LocalDate date, String bp, String sugar, String temp) {
            this.date = date; this.bp = bp; this.sugar = sugar; this.temp = temp;
        }

        @Override
        public String getSummary() {
            return "[Vitals Log] Date: " + date + " | BP: " + bp + " | Sugar: " + sugar + " | Temp: " + temp;
        }
    }
}
