package se.comerit.resurs.controller;

import com.sun.jna.Platform;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.comerit.resurs.jna.JnaEncrypter;

@RestController
@RequestMapping("/api/jna")
public class JNATESTCONTROLLER {

    @GetMapping("/hello")
    public String hello() {
        String base = Platform.isLinux() ? "running linux": "running else";
        return base + "reponsed with "+ JnaEncrypter.hello("i'm different."); }


}
