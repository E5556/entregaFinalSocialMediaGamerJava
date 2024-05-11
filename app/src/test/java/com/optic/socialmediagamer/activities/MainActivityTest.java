package com.optic.socialmediagamer.activities;

import static org.mockito.Mockito.*;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.text.Editable;
import android.text.SpannableStringBuilder;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.textfield.TextInputEditText;
import com.optic.socialmediagamer.activities.MainActivity;
import com.optic.socialmediagamer.providers.AuthProvider;
import com.optic.socialmediagamer.providers.UsersProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import dmax.dialog.SpotsDialog;

@RunWith(MockitoJUnitRunner.class)
public class MainActivityTest {

    @Mock
    private AuthProvider mAuthProvider;

    @Mock
    private UsersProvider mUsersProvider;

    @Mock
    private GoogleSignInClient mGoogleSignInClient;

    @Mock
    private SpotsDialog mDialog;

    @Mock
    private TextView mTextViewRegister;

    @Mock
    private TextInputEditText mTextInputEmail;

    @Mock
    private TextInputEditText mTextInputPassword;

    @Mock
    private Button mButtonLogin;

    private MainActivity mainActivity;

    @Before
    public void setUp() {
        mainActivity = new MainActivity();

        // Inject the mocks into the MainActivity
        mainActivity.mAuthProvider = mAuthProvider;
        mainActivity.mUsersProvider = mUsersProvider;
        mainActivity.mGoogleSignInClient = mGoogleSignInClient;
        mainActivity.mDialog = mDialog;
        mainActivity.mTextViewRegister = mTextViewRegister;
        mainActivity.mTextInputEmail = mTextInputEmail;
        mainActivity.mTextInputPassword = mTextInputPassword;
        mainActivity.mButtonLogin = mButtonLogin;
    }

    @Test
    public void testLogin() {
        // Arrange
        Editable email = new SpannableStringBuilder("e5556@hotmail.com");
        Editable password = new SpannableStringBuilder("123456");
        when(mTextInputEmail.getText()).thenReturn(email);
        when(mTextInputPassword.getText()).thenReturn(password);

        // Act
        mainActivity.login();

        // Assert
        verify(mAuthProvider).login("e5556@hotmail.com", "123456");
        verify(mainActivity).startActivity(any(Intent.class));
    }
}