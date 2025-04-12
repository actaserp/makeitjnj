package mes.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer{

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(new GuiHttpInterceptor()).addPathPatterns("/gui/*");
        //registry.addInterceptor(new ApiHttpInterceptor()).addPathPatterns("/Api/*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8010", "https://jnj.actascld.co.kr/", "https://jnj.actas.co.kr/", "http://jnj.actas.co.kr/" ) // 모든 도메인 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/editorFile/**")
                .addResourceLocations("file:///c:/temp/editorFile/");
    }

}
