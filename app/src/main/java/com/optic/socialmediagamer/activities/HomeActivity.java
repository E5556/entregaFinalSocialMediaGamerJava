package com.optic.socialmediagamer.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.optic.socialmediagamer.R;
import com.optic.socialmediagamer.fragments.ChatsFragment;
import com.optic.socialmediagamer.fragments.FiltersFragment;
import com.optic.socialmediagamer.fragments.HomeFragment;
import com.optic.socialmediagamer.fragments.ProfileFragment;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        openFragment(new HomeFragment());
    }

    // Método para abrir un fragmento en el contenedor
    public void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Listener para la navegación de la barra inferior
    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = item -> {
        if (item.getItemId() == R.id.itemHome) {
            // FRAGMENT HOME
            openFragment(new HomeFragment());
        }
        else if (item.getItemId() == R.id.itemChats) {
            // FRAGMENT CHATS
            openFragment(new ChatsFragment());
        }
        else if (item.getItemId() == R.id.itemFilters) {
            // FRAGMENT FILTROS
            openFragment(new FiltersFragment());
        }
        else if (item.getItemId() == R.id.itemProfile) {
            // FRAGMENT PROFILE
            openFragment(new ProfileFragment());
        }
        return true;
    };
}
