package com.example.gps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.api.ApiClient;
import com.example.gps.api.UserApi;
import com.example.gps.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, emailEditText, nameEditText;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.editTextUsername);
        passwordEditText = findViewById(R.id.editTextPassword);
        emailEditText = findViewById(R.id.editTextEmail);
        nameEditText = findViewById(R.id.editTextName);
        signupButton = findViewById(R.id.buttonSignup);

        // 기본값 자동 입력
        usernameEditText.setText("testuser");
        passwordEditText.setText("1234");
        emailEditText.setText("test@example.com");
        nameEditText.setText("홍길동");

        // 강제 실행
        signup();

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });
    }


    private void signup() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String name = nameEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show();

        // ✅ MapsActivity로 이동
        startActivity(new Intent(MainActivity.this, MapsActivity.class));
        finish(); // 현재 액티비티 종료 (선택)
    }


}


