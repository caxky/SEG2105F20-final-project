package com.example.service_novigrad.ui.login;

import com.example.service_novigrad.accounts.CustomerAccount;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.example.service_novigrad.R;
import com.example.service_novigrad.accounts.EmployeeAccount;
import com.example.service_novigrad.ui.admin.AdminPanelActivity;
import com.example.service_novigrad.ui.admin.AdminServices;
import com.example.service_novigrad.ui.employee.EmployeePanel;
import com.example.service_novigrad.ui.employee.ServiceRequests;
import com.example.service_novigrad.ui.login.LoginViewModel;
import com.example.service_novigrad.ui.login.LoginViewModelFactory;
import com.example.service_novigrad.ui.register.Branch;
import com.example.service_novigrad.ui.register.RegisterActivity;
import com.example.service_novigrad.ui.welcome.WelcomeActivity;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    public static String userName, password; //made a class variable to be able to access later in welcome screen so we can get the account type

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = ViewModelProviders.of(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final Button registerButton = findViewById(R.id.register);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
                finish();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {


                //Checks the Customer Accounts =====================================================================================================================
                DatabaseReference accountsReference = FirebaseDatabase.getInstance().getReference().child("Customer Accounts");
                accountsReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Iterable<DataSnapshot> children = snapshot.getChildren(); //gets an iterable of the customer accounts

                        //iterates through the iterable to find if any customer account fits the description
                        for (DataSnapshot child : children) {
                            CustomerAccount temp = child.getValue(CustomerAccount.class);
                            if (temp.getUsername().equals(usernameEditText.getText().toString()) && temp.getPassword().equals(passwordEditText.getText().toString())) {
                                startActivity(new Intent(view.getContext(), WelcomeActivity.class));
                                userName = usernameEditText.getText().toString();
                                password = passwordEditText.getText().toString();
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                //Checks the Employee Accounts ====================================================================================================================
                accountsReference = FirebaseDatabase.getInstance().getReference().child("Employee Accounts");

                accountsReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Iterable<DataSnapshot> children = snapshot.getChildren();//gets an iterable of the employees accounts

                        //iterates through the iterable to find if any employee account fits the description
                        for (DataSnapshot child : children) {
                            final EmployeeAccount temp = child.getValue(EmployeeAccount.class);
                            if (temp.getUsername().equals(usernameEditText.getText().toString()) && temp.getPassword().equals(passwordEditText.getText().toString())) {
                                DatabaseReference branchesReference = FirebaseDatabase.getInstance().getReference("Branches");
                                branchesReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                         Iterable<DataSnapshot> children = snapshot.getChildren(); //gets an iterable of the service
                                        for (DataSnapshot child: children){
                                            Branch temp2 = child.getValue(Branch.class);
                                            if (temp2.getBranchID() == temp.getBranchID()){
                                                Intent intent = new Intent(view.getContext(), EmployeePanel.class);
                                                intent.putExtra("employeeFirstName", temp.getFirstName());
                                                intent.putExtra("employeeLastName", temp.getLastName());
                                                intent.putExtra("employeeAccountID", temp.getAccountID());
                                                intent.putExtra("branchKey", temp2.getBranchFirebaseKey());
                                                intent.putExtra("branchID", temp.getBranchID());
                                                startActivity(intent);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                                userName = usernameEditText.getText().toString();
                                password = passwordEditText.getText().toString();
                                break;
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
//                accountsReference.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        Iterable<DataSnapshot> children = snapshot.getChildren();//gets an iterable of the employees accounts
//
//                        //iterates through the iterable to find if any employee account fits the description
//                        for (DataSnapshot child : children) {
//                            EmployeeAccount temp = child.getValue(EmployeeAccount.class);
//                            if (temp.getUsername().equals(usernameEditText.getText().toString()) && temp.getPassword().equals(passwordEditText.getText().toString())) {
//                                Intent intent = new Intent(view.getContext(), EmployeePanel.class);
//                                intent.putExtra("employeeFirstName", temp.getFirstName());
//                                intent.putExtra("employeeLastName", temp.getFirstName());
//                                intent.putExtra("employeeAccountID", temp.getAccountID());
//                                intent.putExtra("branchID", temp.getBranchID());
//                                startActivity(intent);
//                                userName = usernameEditText.getText().toString();
//                                password = passwordEditText.getText().toString();
//                                break;
//                            }
//                        }
//
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });



                // Check for Admin Accounts ===========================================================================================================================
                if (usernameEditText.getText().toString().equals("admin") && passwordEditText.getText().toString().equals("admin")) {
                    startActivity(new Intent(view.getContext(), AdminPanelActivity.class));
                }


            }
        });


        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), RegisterActivity.class));
            }
        });

    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}