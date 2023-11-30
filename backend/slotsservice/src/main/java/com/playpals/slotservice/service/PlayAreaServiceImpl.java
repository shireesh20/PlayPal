package com.playpals.slotservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.playpals.slotservice.exception.ResourceNotFoundException;
import com.playpals.slotservice.model.*;
import com.playpals.slotservice.pojo.PlayAreaRequest;
import com.playpals.slotservice.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class PlayAreaServiceImpl implements PlayAreaService {


    @Autowired
    private PlayAreaRepository playAreaRepository;
    @Autowired
    private PlayAreaDocRepository playAreaDocRepository;
    @Autowired
    private PlayAreaSportRepository playAreaSportRepository;
    @Autowired
    private PlayAreaTimingRepository playAreaTimingRepository;

    @Autowired
    private CourtRepository courtsRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSlotRepository eventSlotRepository;

    @Autowired
    private EventUsersRepository eventUsersRepository;



    @Autowired
    private SportsRepository sportsRepository;
    
    @Value("${spring.aws.cloudfront}")
    private String cloudfront;
    
    @Value("${spring.aws.credentials.accessKey}")
    private String accessKey;
    
    @Value("${spring.aws.credentials.secretKey}")
    private String secretKey;

    @Autowired
    private UserRepository userRepository;

    // Add any other required repositories

    public void createPlayArea(PlayAreaRequest playAreaRequest,MultipartFile[] files) throws IOException, Exception {
        // Step 1: Create and save the PlayArea entity
        PlayArea playArea = new PlayArea();
        // Set properties from PlayAreaRequest to PlayArea entity
        playArea.setName(playAreaRequest.getName());
        playArea.setCity(playAreaRequest.getCity());
        User ownerUser = userRepository.findByUsername(playAreaRequest.getOwner());
        if (ownerUser == null) {
            throw new Exception("Owner not found in users table");
        }
        Integer ownerId = ownerUser.getId();
        playArea.setOwner(ownerId);
        playArea.setAddress1(playAreaRequest.getAddress1());
        playArea.setAddress2(playAreaRequest.getAddress2());
        playArea.setState(playAreaRequest.getState());
        playArea.setCountry(playAreaRequest.getCountry());
        playArea.setZipcode(playAreaRequest.getZipcode());
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest = objectMapper.writeValueAsString(playAreaRequest);

// Set the JSON string in the playArea
        playArea.setRequest(jsonRequest);
        
        playArea.setStatus("Requested");

        playArea = playAreaRepository.save(playArea);
        Integer newPlayAreaId = playArea.getId();

        try {
            for (MultipartFile file : files) {
                // Process and save each file
                if (file != null && !file.isEmpty()) {
                    String fileUrl = uploadFileToS3(file);
                    PlayAreaDoc playAreaDoc = new PlayAreaDoc();
                    playAreaDoc.setPlayAreaId(newPlayAreaId);
                    System.out.println("fileurl  " + fileUrl);
                    playAreaDoc.setS3Url(fileUrl);
                    playAreaDoc.setName(playArea.getName());
                    playAreaDoc.setType(getFileExtension(file));
                    System.out.println("file type  " + getFileExtension(file));

                    System.out.println("playAreaDoc  " + playAreaDoc);

                    playAreaDocRepository.save(playAreaDoc);
                }
                System.out.println("play area doc inserted");
            }
        } catch (Exception e) {
            // Handle the exception here
            e.printStackTrace(); // You can replace this with your preferred error handling
        }

        int startTime = playAreaRequest.getStartTime();
        int endTime = playAreaRequest.getEndTime();






        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        playAreaTimingRepository.deleteByPlayAreaId(newPlayAreaId);

        List<PlayAreaTiming> newTimings = new ArrayList<>();

        for (String day : days) {
            PlayAreaTiming newPlayAreaTiming = new PlayAreaTiming();
            newPlayAreaTiming.setPlayAreaId(newPlayAreaId);
            newPlayAreaTiming.setDay(day);
            newPlayAreaTiming.setStartTime(startTime);
            newPlayAreaTiming.setEndTime(endTime);

            newTimings.add(newPlayAreaTiming);
        }

        playAreaTimingRepository.saveAll(newTimings);




        System.out.println("play area timings updated");

        try {
            int numberOfCourtsPerSport = playAreaRequest.getCourts(); // Assuming 4 courts per sport

            // Delete existing courts and play area sports
            courtsRepository.deleteByPlayAreaId(newPlayAreaId);
            playAreaSportRepository.deleteByPlayAreaId(newPlayAreaId);

            // Loop through sport IDs (1 to 8) and create courts
            for (int sportId = 1; sportId <= 1; sportId++) {
                boolean courtExists = courtsRepository.existsByPlayAreaIdAndSportId(newPlayAreaId, sportId);

                if (!courtExists) {
                    for (int courtNumber = 1; courtNumber <= numberOfCourtsPerSport; courtNumber++) {
                        String courtName = "Court " + courtNumber;
                        Courts court = new Courts();
                        court.setPlayAreaId(newPlayAreaId);
                        court.setSportId(sportId);
                        court.setName(courtName);

                        courtsRepository.save(court);
                        System.out.println("Court Inserted: " + courtName);
                        court = null;
                    }
                } else {
                    System.out.println("Courts already exist for Sport ID " + sportId);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Handle exceptions
        }





        try {
            List<Integer> sportIds = new ArrayList<>();

            List<String> sports = playAreaRequest.getSports();
            int numberOfCourts = playAreaRequest.getCourts();
            if (!sports.isEmpty()) {
                for (String sportName : sports) {  // Rename the variable to avoid conflicts
                    PlayAreaSport playAreaSport = new PlayAreaSport();
                    playAreaSport.setPlayAreaId(newPlayAreaId);

                    Sport sport = sportsRepository.getByName(sportName);  // Rename the variable

                    playAreaSport.setSportId(sport.getId());
                    sportIds.add(sport.getId());

                    boolean exists = playAreaSportRepository.existsByPlayAreaIdAndSportId(newPlayAreaId, sport.getId());

                    if (!exists) {
                        // Save the new entry only if it doesn't exist
                        playAreaSportRepository.save(playAreaSport);
                        System.out.println("play Area Sport Inserted");
                    }
                    System.out.println(numberOfCourts);

//                    for (int i = 1; i <= numberOfCourts; i++) {
//                        String courtName = "court " + i;
//
//                        // Check if a court with the same name, play area ID, and sport ID already exists
//                        boolean courtExists = courtsRepository.existsByPlayAreaIdAndSportIdAndName(newPlayAreaId, sport.getId(), courtName);
//                        Courts court = new Courts();
//
//                        if (!courtExists) {
//                            // Create a Courts entity and save it
//                            court.setPlayAreaId(newPlayAreaId);
//                            court.setSportId(sport.getId());
//                            court.setName(courtName);
//
//                            courtsRepository.save(court);
//                            System.out.println("Court Inserted: " + courtName);
//                        } else {
//                            System.out.println("Court already exists: " + courtName);
//                        }
//                    }

                }
            }  else {
                throw new EntityNotFoundException("No sports found with name: " + sports);
            }
        } catch (Exception ex) {
            // Handle unexpected exceptions or log them appropriately
            // logger.error("An unexpected error occurred", ex);
            throw new RuntimeException("An unexpected error occurred", ex);
        }
    }

    // Update PlayArea Service Method
    public void updatePlayArea(Integer playAreaId, PlayAreaRequest playAreaRequest,MultipartFile[] files) throws Exception,IOException {
        // Step 1: Retrieve existing PlayArea entity
        Optional<PlayArea> optionalPlayArea = playAreaRepository.findById(playAreaId);
        if (optionalPlayArea.isPresent()) {
            PlayArea playArea = optionalPlayArea.get();

            System.out.println("update playArea");

            // Step 2: Update the PlayArea entity with new values
            playArea.setName(playAreaRequest.getName());
            playArea.setCity(playAreaRequest.getCity());
            User ownerUser = userRepository.findByUsername(playAreaRequest.getOwner());
            if (ownerUser == null) {
                throw new Exception("Owner not found in users table");
            }
            Integer ownerId = ownerUser.getId();
            playArea.setOwner(ownerId);
            playArea.setAddress1(playAreaRequest.getAddress1());
            playArea.setAddress2(playAreaRequest.getAddress2());
            playArea.setState(playAreaRequest.getState());
            playArea.setCountry(playAreaRequest.getCountry());
            playArea.setZipcode(playAreaRequest.getZipcode());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRequest = objectMapper.writeValueAsString(playAreaRequest);

// Set the JSON string in the playArea
            playArea.setRequest(jsonRequest);


            // Update other fields as needed

            playAreaRepository.save(playArea); // Save the updated PlayArea entity



            try {
                for (MultipartFile file : files) {
                    // Process and save each file
                    if (file != null && !file.isEmpty()) {
                        String fileUrl = uploadFileToS3(file);
                        PlayAreaDoc playAreaDoc = new PlayAreaDoc();
                        playAreaDoc.setPlayAreaId(playAreaId);
                        System.out.println("fileurl  " + fileUrl);
                        playAreaDoc.setS3Url(fileUrl);
                        playAreaDoc.setName(playArea.getName());
                        playAreaDoc.setType(getFileExtension(file));
                        System.out.println("file type  " + getFileExtension(file));

                        System.out.println("playAreaDoc  " + playAreaDoc);

                        playAreaDocRepository.save(playAreaDoc);
                    }
                    System.out.println("play area doc inserted");
                }
            } catch (Exception e) {
                // Handle the exception here
                e.printStackTrace(); // You can replace this with your preferred error handling
            }
            int startTime = playAreaRequest.getStartTime();
            int endTime = playAreaRequest.getEndTime();

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

            playAreaTimingRepository.deleteByPlayAreaId(playAreaId);

            List<PlayAreaTiming> newTimings = new ArrayList<>();

            for (String day : days) {
                PlayAreaTiming newPlayAreaTiming = new PlayAreaTiming();
                newPlayAreaTiming.setPlayAreaId(playAreaId);
                newPlayAreaTiming.setDay(day);
                newPlayAreaTiming.setStartTime(startTime);
                newPlayAreaTiming.setEndTime(endTime);

                newTimings.add(newPlayAreaTiming);
            }

            playAreaTimingRepository.saveAll(newTimings);



            System.out.println("play area timings updated");

            PlayAreaSport playAreaSport = new PlayAreaSport();
            playAreaSport.setPlayAreaId(playAreaId);

            try {
                List<String> sports = playAreaRequest.getSports();

                if (!sports.isEmpty()) {
                    for (String s : sports) {
                        Sport sport = sportsRepository.getByName(s);

                        // Check if the association already exists
                        boolean exists = playAreaSportRepository.existsByPlayAreaIdAndSportId(playAreaId, sport.getId());

                        if (!exists) {
                            // If the association doesn't exist, save the new entry
                            System.out.println("inside exists");
                            playAreaSport.setSportId(sport.getId());
                            playAreaSportRepository.save(playAreaSport);
                        }
                    }
                } else {
                    throw new EntityNotFoundException("No sports found with name: " + sports);
                }
            } catch (EntityNotFoundException ex) {
                // Handle the exception or log it appropriately
                // logger.error("No sports found", ex);
                throw ex;
            } catch (Exception ex) {
                // Handle unexpected exceptions or log them appropriately
                // logger.error("An unexpected error occurred", ex);
                throw new RuntimeException("An unexpected error occurred", ex);
            }

            // Additional logic for updating PlayAreaTiming and PlayAreaSport as needed
        } else {
            throw new EntityNotFoundException("PlayArea not found with ID: " + playAreaId);
        }
    }




    private String uploadFileToS3(MultipartFile file) throws Exception, IOException {
        // AWS S3 Bucket details
        String bucketName = "playpal-dev";
        String key = "uploads/" + file.getOriginalFilename(); // or use a custom key

        // AWS Credentials (You should not hardcode these in production code)
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                accessKey,
                secretKey
        );

        // Create S3 client
        S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.US_EAST_2) // e.g., Region.US_EAST_1
                .build();

        // Upload file to S3
        PutObjectResponse response = s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        // Close the S3 client
        s3.close();



        // Return the file URL (Assuming public access or you can generate a pre-signed URL)
        return "https://" + cloudfront  +"/" +key;
    }

    public static String getFileExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        // Check if the file name is not null and contains a period.
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ""; // No extension found or no file name
        }

        // Get everything after the last period.
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }




    public PlayArea getPlayAreaById(int id) {
        Optional<PlayArea> playAreaOptional = playAreaRepository.findById(id);
        if (playAreaOptional.isPresent()) {
            return playAreaOptional.get();
        } else {
            throw new ResourceNotFoundException("PlayArea not found with id " + id);
        }
    }


    @Transactional
    public void deletePlayArea(Integer playAreaId) {
        try {
            // Step 1: Check if the PlayArea exists
            Optional<PlayArea> playAreaOptional = playAreaRepository.findById(playAreaId);

            if (playAreaOptional.isPresent()) {
                eventSlotRepository.deleteByPlayAreaId(playAreaId);




                List<Integer> eventIds = eventRepository.findByPlayAreaId(playAreaId)
                        .stream()
                        .map(Event::getId)
                        .collect(Collectors.toList());


                eventUsersRepository.deleteByEventIdIn(eventIds);
                eventRepository.deleteByPlayAreaId(playAreaId);


                // Delete associated records in play_area_docs and play_area_timings
                playAreaDocRepository.deleteByPlayAreaId(playAreaId);
                playAreaTimingRepository.deleteByPlayAreaId(playAreaId);

                // Delete associated records in play_area_sports and courts
                playAreaSportRepository.deleteByPlayAreaId(playAreaId);
                courtsRepository.deleteByPlayAreaId(playAreaId);

                // Finally, delete the play area
                playAreaRepository.deleteById(playAreaId);
            } else {
                // Handle the case where the PlayArea with the given ID doesn't exist
                throw new EntityNotFoundException("PlayArea with ID " + playAreaId + " not found.");
            }
        } catch (Exception e) {
            // Handle exceptions as needed
            // For example, log the exception or throw a custom exception
            e.printStackTrace(); // log the exception
            throw new RuntimeException("Error deleting PlayArea: " + e.getMessage());
        }
    }

    public List<PlayArea> getPlayAreasByStatus(String status) {
        return playAreaRepository.findByStatus(status);
    }

    public List<PlayArea> findAll() {
        return playAreaRepository.findAll();
    }


    public List<PlayArea> findByUserName(String userName) {
        // Use the repository method to find play areas by userName
        return playAreaRepository.findByName(userName);
    }

    public List<PlayArea> findByOwnerId(int ownerId) {
        return playAreaRepository.findByOwner(ownerId);
    }


}



