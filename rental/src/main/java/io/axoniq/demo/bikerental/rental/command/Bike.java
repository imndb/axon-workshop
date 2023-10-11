package io.axoniq.demo.bikerental.rental.command;

import io.axoniq.demo.bikerental.coreapi.rental.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.Objects;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class Bike {

    @AggregateIdentifier
    private String bikeId ;

    private String bikeType;

    private String location;
    private String renter;
    private String rentalReference;

    private boolean reservationConfirmed;

    private boolean isAvailable;

    public Bike() {
    }


    @CommandHandler
    public Bike(RegisterBikeCommand command) {

        apply(new BikeRegisteredEvent(command.bikeId(), command.bikeType(), command.location()));

    }
    @EventSourcingHandler
    protected void on(BikeRegisteredEvent event) {
        this.bikeId = event.bikeId();
        this.isAvailable = true;
    }

    @EventSourcingHandler
    protected  void on(BikeReturnedEvent event) {
        this.isAvailable = true;
        this.reservationConfirmed = false;
        this.renter = null;
    }

    @CommandHandler
    public String handle(RequestBikeCommand requestBikeCommand) {
        String rentalReference = UUID.randomUUID().toString();
        if(!this.isAvailable) {
            throw new IllegalStateException("Bike already requested");
        }
        apply(new BikeRequestedEvent(requestBikeCommand.bikeId(), requestBikeCommand.renter(), rentalReference));

        return rentalReference;
    }

    @CommandHandler
    public void handle(ApproveRequestCommand approveRequestCommand) {
        if(!Objects.equals(renter, approveRequestCommand.renter())
                || reservationConfirmed)
            return;
        else
            apply(new BikeInUseEvent(approveRequestCommand.bikeId(), approveRequestCommand.renter()));

    }

    @EventSourcingHandler
    protected void on(BikeRequestedEvent event) {
        this.renter = event.renter();
        this.reservationConfirmed = false;
        this.isAvailable = false;
    }



    @CommandHandler
    public void handle(ReturnBikeCommand command) {
        if(this.isAvailable)
            throw new IllegalStateException("Bike was already returned");
        apply(new BikeReturnedEvent(command.bikeId(), command.location()));
    }
    @CommandHandler
    public void handle(RejectRequestCommand event) {
        if(!Objects.equals(renter, event.renter())
                || reservationConfirmed)
            return;
        else
        apply(new RequestRejectedEvent(event.bikeId()));
    }

    @EventSourcingHandler
    protected void on(RequestRejectedEvent event) {
        this.isAvailable = true;
        this.reservationConfirmed = false;
        this.renter = null;

    }

    @EventSourcingHandler
    protected  void on(BikeInUseEvent event) {
        this.isAvailable = false;
        this.reservationConfirmed = true;
    }


}
