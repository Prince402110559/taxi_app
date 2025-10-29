package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;



import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView email, name, surname;

    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 🔑 1. Get references to the buttons from XML
        Button btnSettings = view.findViewById(R.id.settingsBtn);
        Button btnEditProfile = view.findViewById(R.id.editProfileBtn);
        Button btnAbout = view.findViewById(R.id.aboutBtn);
        Button btnLogout = view.findViewById(R.id.logoutBtn);
         name = view.findViewById(R.id.profileName);
         email = view.findViewById(R.id.profileEmail);
         surname = view.findViewById(R.id.LastName);


        // 🔑 2. Set click listeners
        btnSettings.setOnClickListener(v -> {
            // Open SettingsActivity when clicked
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            // Open EditProfileActivity when clicked
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v ->{
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this.getActivity(), WelcomePage.class);
            startActivity(intent);
        });
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userEmail = documentSnapshot.getString("email");
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            load(firstName,userEmail,lastName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                    });

        }

        return view;
    }
    private void load( String nm, String em, String lm) {
        TextView nameView = name;
        TextView emailView = email;
        TextView surnameView = surname;

        if(nm != null && !nm.isEmpty()){
            nameView.setText(nm);
        }else{
            nameView.setText("john Doe");
        }
        if(em!= null && !em.isEmpty()){
            emailView.setText(em);
        }else{
            emailView.setText("johnDoe@example.com");
        }if(lm != null && !lm.isEmpty()){
            surnameView.setText(lm);
        }else{
            surnameView.setText("Doe");
        }
    }

}
