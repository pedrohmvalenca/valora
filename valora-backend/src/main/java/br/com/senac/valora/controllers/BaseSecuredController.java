package br.com.senac.valora.controllers;

import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("denyAll()")
public abstract class BaseSecuredController {
}
