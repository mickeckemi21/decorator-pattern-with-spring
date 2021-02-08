package io.mickeckemi21.decoratorpatternwithspring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Set;

import static io.mickeckemi21.decoratorpatternwithspring.DecoratorPatternWithSpringApplication.UserDefinedCalculatorServicePrecedenceBeanDefinitionRegistryPostProcessor.NOT_SO_SIMPLE_CORE_CALCULATOR_SERVICE_BEAN_NAME;
import static io.mickeckemi21.decoratorpatternwithspring.DecoratorPatternWithSpringApplication.UserDefinedCalculatorServicePrecedenceBeanDefinitionRegistryPostProcessor.SIMPLE_CALCULATOR_CORE_SERVICE_BEAN_NAME;
import static java.util.Arrays.stream;

@SpringBootApplication
public class DecoratorPatternWithSpringApplication {

    private static final Logger log = LoggerFactory.getLogger(DecoratorPatternWithSpringApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DecoratorPatternWithSpringApplication.class, args);
    }

    @Bean
    CommandLineRunner cmdLineRunner(SimpleCalculator calculator) {
        return args -> {
            log.info(">> CmdLineRunner#run");
            calculator.doCalculation();
            log.info("<< CmdLineRunner#run");
        };
    }

    @Bean(SIMPLE_CALCULATOR_CORE_SERVICE_BEAN_NAME)
    CalculatorService simpleCoreCalculatorService() {
        return new SimpleCoreCalculatorService();
    }

    @Bean(NOT_SO_SIMPLE_CORE_CALCULATOR_SERVICE_BEAN_NAME)
    CalculatorService notSoSimpleCoreCalculatorService() {
        return new NotSoSimpleCoreCalculatorService(simpleCoreCalculatorService());
    }

    @Bean
    static BeanDefinitionRegistryPostProcessor calculatorServiceBeanDefinitionRegistryPostProcessor() {
        return new UserDefinedCalculatorServicePrecedenceBeanDefinitionRegistryPostProcessor();
    }

    static class UserDefinedCalculatorServicePrecedenceBeanDefinitionRegistryPostProcessor
            implements BeanDefinitionRegistryPostProcessor {

        public static final String SIMPLE_CALCULATOR_CORE_SERVICE_BEAN_NAME = "simpleCoreCalculatorService";
        public static final String NOT_SO_SIMPLE_CORE_CALCULATOR_SERVICE_BEAN_NAME = "notSoSimpleCoreCalculatorService";

        public static final String PRIMARY_CORE_CALCULATOR_SERVICE_BEAN_NAME = NOT_SO_SIMPLE_CORE_CALCULATOR_SERVICE_BEAN_NAME;

        public static final Set<String> CORE_CALCULATOR_SERVICE_BEAN_NAMES = Set.of(
                SIMPLE_CALCULATOR_CORE_SERVICE_BEAN_NAME,
                NOT_SO_SIMPLE_CORE_CALCULATOR_SERVICE_BEAN_NAME
        );

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
            // Do nothing
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            final String[] calculatorServiceBeanNames = beanFactory
                    .getBeanNamesForType(CalculatorService.class);
            if (calculatorServiceBeanNames.length == CORE_CALCULATOR_SERVICE_BEAN_NAMES.size()) {
                final BeanDefinition coreCalculatorServiceBeanDefinition = beanFactory
                        .getBeanDefinition(PRIMARY_CORE_CALCULATOR_SERVICE_BEAN_NAME);
                coreCalculatorServiceBeanDefinition.setPrimary(true);
            } else if (calculatorServiceBeanNames.length == CORE_CALCULATOR_SERVICE_BEAN_NAMES.size() + 1) {
                stream(calculatorServiceBeanNames)
                        .filter(beanName -> !CORE_CALCULATOR_SERVICE_BEAN_NAMES.contains(beanName))
                        .map(beanFactory::getBeanDefinition)
                        .filter(beanDefinition -> !beanDefinition.isPrimary())
                        .findFirst()
                        .ifPresent(beanDefinition -> beanDefinition.setPrimary(true));
            }
        }
    }

}

interface CalculatorService {
    void calculate();
}

@Component
class SimpleCalculator {

    private static final Logger log = LoggerFactory.getLogger(SimpleCalculator.class);

    private final CalculatorService calculatorService;

    public SimpleCalculator(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    void doCalculation() {
        log.info(">> SimpleCalculator#doCalculation");
        this.calculatorService.calculate();
        log.info("<< SimpleCalculator#doCalculation");
    }

}

class SimpleCoreCalculatorService implements CalculatorService {

    private static final Logger log = LoggerFactory.getLogger(SimpleCoreCalculatorService.class);

    @Override
    public void calculate() {
        log.info(">> SimpleCalculator#calculate");
        log.info("<< SimpleCalculator#calculate");
    }

}

class NotSoSimpleCoreCalculatorService implements CalculatorService {

    private static final Logger log = LoggerFactory.getLogger(NotSoSimpleCoreCalculatorService.class);

    private final CalculatorService calculatorService;

    public NotSoSimpleCoreCalculatorService(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @Override
    public void calculate() {
        log.info(">> NotSoSimpleCalculatorService#calculate");
        this.calculatorService.calculate();
        log.info("<< NotSoSimpleCalculatorService#calculate");
    }

}

@Service
class UserDefinedCalculatorService implements CalculatorService {

    private static final Logger log = LoggerFactory.getLogger(UserDefinedCalculatorService.class);

    private final CalculatorService calculatorService;

    public UserDefinedCalculatorService(
            @Qualifier("notSoSimpleCoreCalculatorService") CalculatorService calculatorService
    ) {
        this.calculatorService = calculatorService;
    }

    @Override
    public void calculate() {
        log.info(">> UserSpecificCalculatorService#calculate");
        this.calculatorService.calculate();
        log.info("<< UserSpecificCalculatorService#calculate");
    }
}
