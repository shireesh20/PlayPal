import React, { useEffect, useState } from "react";
import { Container, Button, Row, Col, Carousel, Alert } from "react-bootstrap";
import { useParams } from "react-router-dom";
import { useNavigate } from "react-router-dom";
// import ChatRoom from '../ChatRoom/ChatRoom.js'
import * as urls from "./config";

const MyEventDetails = ({ venues, loggedInUser }) => {
  const { id } = useParams();
  const [events, setEvents] = useState(venues);
  const [chatRoomId, setChatRoomId] = useState();
  const navigate = useNavigate();
  useEffect(() => {
    if (venues) {
      setEvents(venues);
      console.log("Ikkada ", events);
    }
  }, [venues]);

  if (!events) {
    return <div>Error loading data</div>;
  }

  const event = events.find((venue) => venue.id === parseInt(id, 10));

  // const chatRoomId="6d19f14e-a9a8-4b11-9b54-746feb790fd4";

  const handleLeaveEvent = async () => {
    try {
      console.log(id, loggedInUser);
      const response = await fetch(urls.leaveEvent, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          event_id: id,
          user_id: loggedInUser,
        }),
      });

      if (response.ok) {
        // Handle successful leave event logic, e.g., refresh the page or update state
        console.log("Successfully left the event");
        navigate("/playerHome/myEvents");
      } else {
        console.error("Error leaving the event:", response.statusText);
      }
    } catch (error) {
      console.error("Error leaving the event:", error);
    }
  };

  return (
    <Container fluid className="mt-4">
      <Container className="main-container rounded p-4 bg-light" style={{ maxWidth: "80%", margin: "auto" }}>
        <h2 className="mb-4">Event Details</h2>
        <Row>
          <div style={{ width: "400px", height: "300px", overflow: "hidden", borderRadius: "10px" }}>
            <Carousel interval={null} style={{ width: "100%", height: "100%" }}>
              {event.photoUrl.map((url, index) => (
                <Carousel.Item key={index}>
                  <img
                    className="d-block w-100 img-fluid rounded"
                    src={url}
                    alt={`Photo ${index + 1}`}
                    style={{ objectFit: "cover", width: "100%", height: "100%", borderRadius: "10px" }}
                  />
                </Carousel.Item>
              ))}
            </Carousel>
          </div>
          <br />
          <div className="details-container rounded bg-light">
            <h3>{event.name}</h3>
            <dl className="row mt-4">
              <dt className="col-sm-4">Event Name:</dt>
              <dd className="col-sm-8">{event.event_name}</dd>

              <dt className="col-sm-4">Sport Type:</dt>
              <dd className="col-sm-8">{event.sportType}</dd>

              <dt className="col-sm-4">Date:</dt>
              <dd className="col-sm-8">{new Date(event.event_slot_date).toISOString().split("T")[0]}</dd>

              <dt className="col-sm-4">Venue:</dt>
              <dd className="col-sm-8">{event.name}</dd>

              <dt className="col-sm-4">Address:</dt>
              <dd className="col-sm-8">{event.address1}</dd>

              <dt className="col-sm-4">Country:</dt>
              <dd className="col-sm-8">{event.country}</dd>

              <dt className="col-sm-4">Court ID:</dt>
              <dd className="col-sm-8">{event.court_id}</dd>

              <dt className="col-sm-4">Created By:</dt>
              <dd className="col-sm-8">{event.created_user}</dd>

              <dt className="col-sm-4">Current Pool Size:</dt>
              <dd
                className="col-sm-8"
                style={{ color: event.current_pool_size < event.pool_size * 0.5 ? "green" : "red" }}
              >
                {event.current_pool_size} / {event.pool_size}
              </dd>
              <dt className="col-sm-4">Players:</dt>
              <dd className="col-sm-8">{event.players.join(", ")}</dd>

              {/* Add more details as needed */}
            </dl>
            <Button variant="danger" disabled={event.created_user === loggedInUser} onClick={handleLeaveEvent}>
              Leave the event
            </Button>
          </div>
        </Row>
      </Container>
    </Container>
  );
};

export default MyEventDetails;
