package com.dsd.reservationsystem.service;

import com.dsd.reservationsystem.database.Db;
import com.dsd.reservationsystem.models.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class AppointmentService {
    private Db database;
    private EmailService emailService;

    private CustomerService customerService;

    private TimeSlotsService timeSlotsService;

    public AppointmentService(Db database, EmailService emailService, CustomerService customerService, TimeSlotsService timeSlotsService) {
        this.database = database;
        this.emailService = emailService;
        this.customerService = customerService;
        this.timeSlotsService = timeSlotsService;
    }


    public Appointment saveAppointment(AppointmentPostRequest appointment) {


        //existing or new customer
        Customer customer;
        String customerEmail = appointment.getCustomerInfo().getEmail();
        AppointmentPostRequest.CustomerInfo customerInfo = appointment.getCustomerInfo();
        AppointmentPostRequest.AppointmentTime appointmentTime = appointment.getAppointmentTime();

        //try to find customer info by email
        try {

            //get customer by email
            Optional<Customer> foundCustomer = this.customerService.getCustomerByEmail(customerEmail);


            //no customer found. Make new entry and return it
            if (foundCustomer.isEmpty()) {

                //create new customer from request
                Customer newCustomer = new Customer();
                newCustomer.setAddress(customerInfo.getAddress());
                newCustomer.setEmail(customerInfo.getEmail());
                newCustomer.setName(customerInfo.getName());
                newCustomer.setPhoneNumber(customerInfo.getPhoneNumber());


                //create customer in database
                customer = this.customerService.createCustomer(newCustomer);

                System.out.println("createdCustomer");
                System.out.println(customer);
            } else {

                customer = foundCustomer.get();
                System.out.println("customer found");
                System.out.println(customer);
            }

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {

            throw new RuntimeException(e);
        }


//        create new appointment from request
        Appointment newAppointment = new Appointment();
        newAppointment.setTimeSlot(appointmentTime.getTimeSlot());
        newAppointment.setDate(appointmentTime.getDay());
        newAppointment.setServiceId(customerInfo.getServiceId());
        newAppointment.setConfirmationNumber(UUID.randomUUID().toString());
        newAppointment.setStatus("PENDING");

        //update appointments on customer
        customer.addAppointment(newAppointment);

        System.out.println("customer new data");
        System.out.println(customer);

        //update customerInfo database with customer changes
        try {
            CollectionReference customersCollection = database.collection("customerInfo");
            customersCollection.document(customer.getId()).set(customer);

        } catch (Exception e) {
            System.out.println("failed to update customerInfo data");
            throw new RuntimeException("failed to update customerInfo data");
        }


        //update timeSlots database
        try {


            String date = newAppointment.getDate();

            //update timeslots with customer id
            timeSlotsService.updateDayTimeslot(customer.getId(), date, appointmentTime.getTimeSlot());

        } catch (Exception e) {
            System.out.println("failed to update timeslots data");
            throw new RuntimeException("failed to update timeslots data");
        }


        return newAppointment;
    }

    public Appointment addAppointmentToCustomer(String customerId, Appointment appointment) {
        // Fetch the customer by ID
        Customer customer = database.getCustomerById(customerId);
        if (customer != null) {
            // Add the new appointment to the customer's list of appointments
            List<Appointment> appointments = customer.getAppointments();
            appointments.add(appointment);
            customer.setAppointments(appointments);

            // Save the customer back to the database
            database.createCustomer(customer);

            // Add the appointment as a time slot for the given date
            DaySchedule daySchedule;
//            try {
//                daySchedule = database.getTimeSlotsForDay(appointment.getDay());
//            } catch (Exception e) {
//                e.printStackTrace();
//                return null;
//            }
//            daySchedule.appointments().put(appointment.getTimeSlot(), appointment);
//            database.updateTimeSlotsForDay(appointment.getDay(), daySchedule.appointments());

            return appointment;
        } else {
            return null;
        }
    }

    public List<Appointment> getAppointmentsForDay(String date) throws ExecutionException, InterruptedException {
        System.out.println("get appointment for day");
        List list = new ArrayList();
        list.add(new Appointment());
        Map<String, Object> daysTimeSlots = database.getAppointmentsForDay(date);

        return list;
    }

}

// // todo find user by there email if they exists update customer info with new
// // appointment
// Query query = database.collection("customerInfo").whereEqualTo("email",
// appointment.getCustomerInfo().getEmail());
// ApiFuture<QuerySnapshot> results = query.get();

// try {

// QuerySnapshot documents = results.get();
// List data = documents.getDocuments();
// System.out.println(data);

// } catch (ExecutionException e) {
// throw new RuntimeException(e);
// } catch (InterruptedException e) {
// throw new RuntimeException(e);
// } catch (Exception e) {
// System.out.printf("unhandled exeption getting customer info");
// }

// todo push user id into timeslot