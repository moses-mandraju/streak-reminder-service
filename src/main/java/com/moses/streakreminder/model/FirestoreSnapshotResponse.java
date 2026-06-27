package com.moses.streakreminder.model;

import lombok.AllArgsConstructor;
import lombok.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreSnapshotResponse {

    private Integer totalUsers;

    private List<UserSnapshot> users;

}