import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 tests for Geotext -- one-to-one mapping to every Python test in
 * tests-geotext/test_geotext.py.
 */
public class GeotextTest {

    // =======================================================================
    // test_cities -> Geotext (cities)
    // =======================================================================

    @Test // test_cities -> Geotext.cities
    void testCities() {
        String text = "São Paulo é a capital do estado de São Paulo. As cidades de Barueri\n"
                + "                  e Carapicuíba fazem parte da Grade São Paulo. O Rio de Janeiro\n"
                + "                  continua lindo. No carnaval eu vou para Salvador. No reveillon eu \n"
                + "                  quero ir para Santos.";
        List<String> result = new Geotext(text).cities;
        List<String> expected = Arrays.asList(
                "São Paulo", "São Paulo", "Barueri", "Carapicuíba",
                "Rio de Janeiro", "Salvador", "Santos");
        assertEquals(expected, result);

        String brazilliansNortheastCapitals = "As capitais do nordeste brasileiro são:\n"
                + "                                            Salvador na Bahia, \n"
                + "                                            Recife em Pernambuco, \n"
                + "                                            Natal fica no Rio Grande do Norte, \n"
                + "                                            João Pessoa fica na Paraíba, \n"
                + "                                            Fortaleza fica no Ceará, \n"
                + "                                            Teresina no Piauí, \n"
                + "                                            Aracaju em Sergipe,\n"
                + "                                            Maceió em Alagoas e \n"
                + "                                            São Luís no Maranhão.";
        result = new Geotext(brazilliansNortheastCapitals).cities;
        // PS: 'Rio Grande' is not a northeast city, but is a brazilian city
        expected = Arrays.asList(
                "Salvador", "Recife", "Natal", "Rio Grande", "João Pessoa",
                "Fortaleza", "Teresina", "Aracaju", "Maceió", "São Luís");
        assertEquals(expected, result);

        String brazilliansNorthCapitals = "As capitais dos estados do norte brasileiro são: \n"
                + "                                        Manaus no Amazonas, \n"
                + "                                        Palmas em Tocantins,\n"
                + "                                        Belém no Pará,\n"
                + "                                        Acre no Rio Branco.";
        result = new Geotext(brazilliansNorthCapitals).cities;
        expected = Arrays.asList("Manaus", "Palmas", "Belém", "Rio Branco");
        assertEquals(expected, result);

        String brazilliansSoutheastCapitals = "As capitais da região sudeste do Brasil são:\n"
                + "                                            Rio de Janeiro no Rio de Janeiro,\n"
                + "                                            São Paulo em São Paulo,\n"
                + "                                            Belo Horizonte em Minas Gerais,\n"
                + "                                            Vitória no Espírito Santo";
        result = new Geotext(brazilliansSoutheastCapitals).cities;
        // 'Rio de Janeiro' and 'Sao Paulo' city and state name are the same, so appears 2 times
        expected = Arrays.asList(
                "Rio de Janeiro", "Rio de Janeiro", "São Paulo", "São Paulo",
                "Belo Horizonte", "Vitória");
        assertEquals(expected, result);

        String brazilliansCentralCapitals = "As capitais da região centro-oeste do Brasil são: \n"
                + "                                          Goiânia em Goiás, \n"
                + "                                          Brasília no Distrito Federal,\n"
                + "                                          Campo Grande no Mato Grosso do Sul,\n"
                + "                                          Cuiabá no Mato Grosso.";
        result = new Geotext(brazilliansCentralCapitals).cities;
        expected = Arrays.asList(
                "Goiânia", "Goiás", "Brasília", "Campo Grande", "Cuiabá");
        assertEquals(expected, result);

        String brazilliansSouthCapitals = "As capitais da região sul são:\n"
                + "                                        Porto Alegre no Rio Grande do Sul,\n"
                + "                                        Floripa em Santa Catarina, \n"
                + "                                        Curitiba no Paraná";
        result = new Geotext(brazilliansSouthCapitals).cities;
        // PS: 'Rio Grande' is not a south city, but is a brazilian city
        expected = Arrays.asList(
                "Porto Alegre", "Rio Grande", "Santa Catarina", "Curitiba", "Paraná");
        assertEquals(expected, result);

        result = new Geotext("Rio de Janeiro y Havana", "BR").cities;
        expected = Arrays.asList("Rio de Janeiro");
        assertEquals(expected, result);
    }

    // =======================================================================
    // test_nationalities -> Geotext.nationalities
    // =======================================================================

    @Test // test_nationalities -> Geotext.nationalities
    void testNationalities() {
        String text = "Japanese people like anime. French people often drink wine. "
                + "Chinese people enjoy fireworks.";
        List<String> result = new Geotext(text).nationalities;
        List<String> expected = Arrays.asList("Japanese", "French", "Chinese");
        assertEquals(expected, result);
    }

    // =======================================================================
    // test_countries -> Geotext.countries
    // =======================================================================

    @Test // test_countries -> Geotext.countries
    void testCountries() {
        String text = "That was fertile ground for the emergence of various forms of\n"
                + "                  totalitarian governments such as Japan, Italy,\n"
                + "                  and Germany, as well as other countries";
        List<String> result = new Geotext(text).countries;
        List<String> expected = Arrays.asList("Japan", "Italy", "Germany");
        assertEquals(expected, result);
    }

    // =======================================================================
    // test_country_mentions -> Geotext.country_mentions
    // =======================================================================

    @Test // test_country_mentions -> Geotext.country_mentions
    void testCountryMentions() {
        String text = "I would like to visit Lima, Dublin and Moscow (Russia).";
        Map<String, Integer> result = new Geotext(text).countryMentions;
        Map<String, Integer> expected = new HashMap<>();
        expected.put("PE", 1);
        expected.put("IE", 1);
        expected.put("RU", 2);
        assertEquals(expected, result);
    }
}
