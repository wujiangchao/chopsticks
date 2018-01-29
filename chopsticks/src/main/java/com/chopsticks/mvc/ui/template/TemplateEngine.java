package com.chopsticks.mvc.ui.template;

import java.io.Writer;

import com.chopsticks.exception.TemplateException;
import com.chopsticks.mvc.ui.ModelAndView;

/**
 * TemplateEngine Interface, For view layer to display data
 *
 * @since 1.5
 */
public interface TemplateEngine {

    /**
     * Render a template file to the client
     *
     * @param modelAndView ModelAndView instance, contains view name and data model
     * @param writer       writer instance
     * @throws TemplateException throw TemplateException when rendering a template
     */
    void render(ModelAndView modelAndView, Writer writer) throws TemplateException;

}