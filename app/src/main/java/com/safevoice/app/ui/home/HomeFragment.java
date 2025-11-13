package com.safevoice.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safevoice.app.KycActivity;
import com.safevoice.app.LoginActivity; // Import the new LoginActivity
import com.safevoice.app.R;
import com.safevoice.app.databinding.FragmentHomeBinding;
import com.safevoice.app.services.VoiceRecognitionService;

/**
 * The fragment for the "Home" screen.
 * It provides controls to start/stop the listening service and to verify the user's identity.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment using view binding.
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up the click listener for the service toggle button.
        binding.buttonToggleService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    stopVoiceService();
                } else {
                    startVoiceService();
                }
                // Update the UI immediately after the button is clicked.
                updateServiceStatusUI();
            }
        });

        // Set up the click listener for the identity verification button.
        binding.buttonVerifyIdentity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // --- THIS IS THE CRITICAL FIX ---
                // First, check if a user is already signed in.
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    // If user is signed in, proceed directly to KYC.
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), KycActivity.class);
                        startActivity(intent);
                    }
                } else {
                    // If no user is signed in, force them to log in first.
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Please sign in to verify your identity.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update the UI every time the fragment becomes visible to ensure it's up-to-date.
        updateServiceStatusUI();
        updateVerificationStatusUI();
    }

    /**
     * Checks the status of the VoiceRecognitionService and updates the UI elements accordingly.
     */
    private void updateServiceStatusUI() {
        if (isServiceRunning()) {
            binding.textServiceStatus.setText(R.string.home_status_listening);
            binding.buttonToggleService.setText(R.string.home_stop_service_button);
        } else {
            binding.textServiceStatus.setText(R.string.home_status_stopped);
            binding.buttonToggleService.setText(R.string.home_start_service_button);
        }
    }

    /**
     * Checks the user's verification status from Firestore and updates the UI.
     */
    private void updateVerificationStatusUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, now check their verification status in Firestore.
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                boolean isVerified = document.getBoolean("isVerified") != null && document.getBoolean("isVerified");
                                String verifiedName = document.getString("verifiedName");

                                if (isVerified && verifiedName != null) {
                                    // User is verified, show their name and hide the button.
                                    String statusText = getString(R.string.home_verification_status_verified, verifiedName);
                                    binding.textVerificationStatus.setText(statusText);
                                    binding.buttonVerifyIdentity.setVisibility(View.GONE);
                                } else {
                                    // User is logged in but not verified.
                                    binding.textVerificationStatus.setText(R.string.home_verification_status);
                                    binding.buttonVerifyIdentity.setVisibility(View.VISIBLE);
                                }
                            } else {
                                // Error fetching document or document doesn't exist.
                                Log.w(TAG, "Failed to fetch user document.", task.getException());
                                binding.textVerificationStatus.setText(R.string.home_verification_status);
                                binding.buttonVerifyIdentity.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        } else {
            // No user is logged in. Show the default "Not Verified" state.
            binding.textVerificationStatus.setText(R.string.home_verification_status);
            binding.buttonVerifyIdentity.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Starts the background voice recognition service.
     */
    private void startVoiceService() {
        if (getActivity() != null) {
            Intent serviceIntent = new Intent(getActivity(), VoiceRecognitionService.class);
            getActivity().startService(serviceIntent);
        }
    }

    /**
     * Stops the background voice recognition service.
     */
    private void stopVoiceService() {
        if (getActivity() != null) {
            Intent serviceIntent = new Intent(getActivity(), VoiceRecognitionService.class);
            getActivity().stopService(serviceIntent);
        }
    }

    /**
     * A simple method to check if the service is running.
     * This relies on a static variable in the service class itself.
     *
     * @return true if the service is currently running, false otherwise.
     */
    private boolean isServiceRunning() {
        return VoiceRecognitionService.isServiceRunning;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the binding object to prevent memory leaks.
        binding = null;
    }
}