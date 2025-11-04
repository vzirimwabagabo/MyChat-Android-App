// File: app/src/main/java/com/example/mychat/MainActivity.java
package com.example.mychat;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int SIGN_IN_REQUEST_CODE = 1;
    private FirebaseListAdapter<ChatMessage> adapter;

    private static final int VIEW_TYPE_MESSAGE_SENT = 0;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Collections.singletonList(new AuthUI.IdpConfig.EmailBuilder().build()))
                    .build(), SIGN_IN_REQUEST_CODE);
        } else {
            // Check if display name is null and handle it
            String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Anonymous";
            }
            Toast.makeText(this, "Welcome " + displayName, Toast.LENGTH_LONG).show();
            displayChatMessages();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        EditText input = findViewById(R.id.input);

        fab.setOnClickListener(view -> {
            String message = input.getText().toString().trim();
            if (!message.isEmpty()) {
                String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "Anonymous";
                }
                FirebaseDatabase.getInstance().getReference().push().setValue(
                        new ChatMessage(message, displayName)
                );
                input.setText("");
            }
        });
    }

    private void displayChatMessages() {
        ListView listOfMessages = findViewById(R.id.list_of_messages);

        Query query = FirebaseDatabase.getInstance().getReference();
        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .setLayout(R.layout.message) // Dummy layout
                .build();

        adapter = new FirebaseListAdapter<ChatMessage>(options) {

            // *** FIX 1: Tell the adapter there are two different view types. ***
            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                // Ensure current user and display name are not null to prevent crashes
                if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                        getItem(position).getMessageUser().equals(FirebaseAuth.getInstance().getCurrentUser().getDisplayName())) {
                    return VIEW_TYPE_MESSAGE_SENT;
                }
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                int viewType = getItemViewType(position);
                View view = convertView;

                // *** FIX 2: Inflate a new view if the recycled view is of the wrong type. ***
                // We check if the view is null OR if its tag doesn't match the required view type.
                if (view == null || (int)view.getTag() != viewType) {
                    if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                        view = getLayoutInflater().inflate(R.layout.item_chat_outgoing, parent, false);
                    } else { // VIEW_TYPE_MESSAGE_RECEIVED
                        view = getLayoutInflater().inflate(R.layout.item_chat_incoming, parent, false);
                    }
                    // We use a tag to remember the type of this view.
                    view.setTag(viewType);
                }

                // Now that we have the correct view, we can populate it.
                ChatMessage model = getItem(position);

                TextView messageText = view.findViewById(R.id.message_text);
                TextView messageTime = view.findViewById(R.id.message_time);
                messageText.setText(model.getMessageText());
                messageTime.setText(DateFormat.format("h:mm a", model.getMessageTime()));

                if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                    TextView messageUser = view.findViewById(R.id.message_user);
                    messageUser.setText(model.getMessageUser());
                }

                return view;
            }

            @Override
            protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
                // This method is now completely empty because we've moved all the logic to getView().
            }
        };
        // Auto-scroll to the bottom
        listOfMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listOfMessages.setStackFromBottom(true);
        listOfMessages.setAdapter(adapter);
    }

    // --- Lifecycle and Menu methods (remain the same) ---
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Successfully signed in. Welcome!", Toast.LENGTH_LONG).show();
                displayChatMessages();
            } else {
                Toast.makeText(this, "We couldn't sign you in. Please try again later.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
                Toast.makeText(MainActivity.this, "You have been signed out.", Toast.LENGTH_LONG).show();
                finish();
            });
        }
        return super.onOptionsItemSelected(item);
    }
}
