package com.optic.socialmediagamer.activities;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.view.View;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.optic.socialmediagamer.R;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class esspreso1 {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void esspreso1() {
        ViewInteraction editText = onView(
                allOf(withId(R.id.textInputEmail), withText("Correo electronico"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText.check(matches(withText("prueba11")));

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.textInputPassword), withText("Contrase�a"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText2.check(matches(withText("456789")));

        ViewInteraction button = onView(
                allOf(withId(R.id.btnLogin), withText("INICIAR SESION"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class))),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction button2 = onView(
                allOf(withId(R.id.btnLogin), withText("INICIAR SESION"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class))),
                        isDisplayed()));
        button2.check(matches(isDisplayed()));

        ViewInteraction textView = onView(
                allOf(withId(R.id.textViewRegister), withText("REGISTRATE AQUI"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class))),
                        isDisplayed()));
        textView.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.textViewRegister), withText("REGISTRATE AQUI"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class))),
                        isDisplayed()));
        textView2.check(matches(isDisplayed()));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.textViewRegister), withText("REGISTRATE AQUI"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class))),
                        isDisplayed()));
        textView3.check(matches(isDisplayed()));

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.textInputUsername), withText("Nombre de usuario"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText3.check(matches(withText("prueba expresso")));

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.textInputEmail), withText("Correo electronico"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText4.check(matches(withText("pruebaesspresso@hotmail.com")));

        ViewInteraction editText5 = onView(
                allOf(withId(R.id.textInputPassword), withText("Contrase�a"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText5.check(matches(withText("123456")));

        ViewInteraction editText6 = onView(
                allOf(withId(R.id.textInputConfirmPassword), withText("Confirmar Contrase�a"),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class))),
                        isDisplayed()));
        editText6.check(matches(withText("123456")));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.btnRegister), withText("REGISTRARSE"),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()));
        button3.check(matches(isDisplayed()));

        ViewInteraction button4 = onView(
                allOf(withId(R.id.btnRegister), withText("REGISTRARSE"),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()));
        button4.check(matches(isDisplayed()));
    }
}
